package main

import (
	"bytes"
	"encoding/hex"
	"fmt"
	"os"
	"sync"
	"time"

	"github.com/charmbracelet/log"
	"github.com/go-pdf/fpdf"
	"github.com/joinself/self-go-sdk/account"
	"github.com/joinself/self-go-sdk/credential"
	"github.com/joinself/self-go-sdk/event"
	"github.com/joinself/self-go-sdk/keypair/signing"
	"github.com/joinself/self-go-sdk/message"
	"github.com/joinself/self-go-sdk/object"
)

var requests sync.Map

func main() {
	// initialize an account that will be used to interact with self and other entities on
	// the network. the account provides storage of all cryptographic key material, as well
	// as credentials and all state used for e2e encrypted messaging with other entitites
	cfg := &account.Config{
		// provide a secure storage key that will  be used to encrypt your local account
		// state. this should be replaced with a securely generated key!
		StorageKey: make([]byte, 32),
		// provide a storage path to the directory where your local account state will be
		// stored
		StoragePath: "./storage",
		// provide an environment to target [Develop, Sandbox]
		Environment: account.TargetSandbox,
		// provide the level of log granularity [Error, Warn, Info, Debug, Trace]
		LogLevel: account.LogWarn,
		// specify callbacks to handle events
		Callbacks: account.Callbacks{
			// invoked when the messaging socket connects
			OnConnect: func(selfAccount *account.Account) {
				log.Info("messaging socket connected")
			},
			// invoked when the messaging socket disconnects. if there is no error
			OnDisconnect: func(selfAccount *account.Account, err error) {
				if err != nil {
					log.Warn("messaging socket disconnected", "error", err)
				} else {
					log.Info("messaging socket disconnected")
				}
			},
			// invoked when there is a response to a discovery request from a new address.
			OnWelcome: func(selfAccount *account.Account, wlc *event.Welcome) {
				// we have received a response to our discovery request that is from a new
				// user/address that we do not have an  end to end encrypted session.
				// accept the invite to join the encrypted group created by the user.
				groupAddress, err := selfAccount.ConnectionAccept(
					wlc.ToAddress(),
					wlc.Welcome(),
				)

				if err != nil {
					log.Warn("failed to accept connection to encrypted group", "error", err.Error())
					return
				}

				log.Info(
					"accepted connection encrypted group",
					"from", wlc.FromAddress().String(),
					"group", groupAddress.String(),
				)
			},
			// invoked when there is a message sent to an encrypted group we are subscribed to
			OnMessage: func(selfAccount *account.Account, msg *event.Message) {
				switch event.ContentTypeOf(msg) {
				case message.ContentTypeDiscoveryResponse:
					log.Info(
						"received response to discovery request",
						"from", msg.FromAddress().String(),
						"requestId", hex.EncodeToString(msg.ID()),
					)

					discoveryResponse, err := message.DecodeDiscoveryResponse(msg.Content())
					if err != nil {
						log.Warn("failed to decode discovery response", "error", err)
						return
					}

					completer, ok := requests.LoadAndDelete(hex.EncodeToString(discoveryResponse.ResponseTo()))
					if !ok {
						log.Warn(
							"received response to unknown request",
							"requestId", hex.EncodeToString(msg.ID()),
							"responseTo", hex.EncodeToString(discoveryResponse.ResponseTo()),
						)
						return
					}

					completer.(chan *signing.PublicKey) <- msg.FromAddress()
				case message.ContentTypeCredentialVerificationResponse:
					log.Info(
						"received response to credential verification request",
						"from", msg.FromAddress().String(),
						"requestId", hex.EncodeToString(msg.ID()),
					)

					credentialVerificationResponse, err := message.DecodeCredentialVerificationResponse(msg.Content())
					if err != nil {
						log.Warn("failed to decode discovery response", "error", err)
						return
					}

					completer, ok := requests.LoadAndDelete(hex.EncodeToString(credentialVerificationResponse.ResponseTo()))
					if !ok {
						log.Warn(
							"received response to unknown request",
							"requestId", hex.EncodeToString(msg.ID()),
							"responseTo", hex.EncodeToString(credentialVerificationResponse.ResponseTo()),
						)
						return
					}

					completer.(chan *message.CredentialVerificationResponse) <- credentialVerificationResponse
				}
			},
		},
	}

	log.Info("initializing self account")

	// initialize and load the account
	selfAccount, err := account.New(cfg)
	if err != nil {
		log.Fatal("failed to initialize account", "error", err)
	}

	// TODO : this will look slightly different in production.
	// right now, we can just open an inbox to send and receive
	// messages from it. In the future we will hide some of this
	// and do proper linking with the application identity.
	// NB: this does not need to happen every time we start the SDK,
	// only once!
	inboxAddress, err := selfAccount.InboxOpen()
	if err != nil {
		log.Fatal("failed to open account inbox", "error", err)
	}

	log.Info("initialized account success")

	for {
		// to determine which user we are interacting with, we can generate a
		// discovery request and encode it to a qr code that your users can scan.
		// as we may not have interacted with this user before, we need to prepare
		// information they need to establish an encrypted group

		// get a key package that the responder can use to create an encryped group
		// for us, if there is not already an existing one.
		keyPackage, err := selfAccount.ConnectionNegotiateOutOfBand(
			inboxAddress,
			time.Now().Add(time.Minute*5),
		)

		if err != nil {
			log.Fatal("failed to generate key package", "error", err)
		}

		// build the key package into a discovery request
		content, err := message.NewDiscoveryRequest().
			KeyPackage(keyPackage).
			Expires(time.Now().Add(time.Minute * 5)).
			Finish()

		if err != nil {
			log.Fatal("failed to build discovery request", "error", err)
		}

		// create a channel to track the response from our qr code
		discoveryCompleter := make(chan *signing.PublicKey, 1)

		requests.Store(
			hex.EncodeToString(content.ID()),
			discoveryCompleter,
		)

		// encode it as a QR code. This can be encoded as either an SVG
		// for use in rendering on a web page, or Unicode, for encoding
		// in text based environments like a terminal
		qrCode, err := event.NewAnonymousMessage(content).
			EncodeToQR(event.QREncodingUnicode)

		if err != nil {
			log.Fatal("failed to encode anonymous message", "error", err)
		}

		log.Info("scan the qr code to complete the discovery request")

		fmt.Println(string(qrCode))

		log.Info(
			"waiting for response to discovery request",
			"requestId", hex.EncodeToString(content.ID()),
		)

		responderAddress := <-discoveryCompleter

		// Read the terms.pdf file
		pdf := fpdf.New("P", "mm", "A4", "")
		pdf.AddPage()
		pdf.SetFont("Arial", "B", 16)
		pdf.Cell(40, 10, "Agreement")

		agreementBuf := bytes.NewBuffer(make([]byte, 1024))

		err = pdf.Output(agreementBuf)
		if err != nil {
			log.Fatal("failed to build agreement pdf", "error", err.Error())
		}

		agreementTerms, err := object.New(
			"application/pdf",
			agreementBuf.Bytes(),
		)

		if err != nil {
			log.Error("failed to encrypt object", "error", err)
		}

		err = selfAccount.ObjectUpload(
			inboxAddress,
			agreementTerms,
			false,
		)

		if err != nil {
			log.Error("failed to upload object", "error", err)
		}

		// create the body of the credential agreement
		// when validating the agreement, any validator will be
		// required to ensure they have been presented signed
		// credentials for all parties
		claims := map[string]interface{}{
			"termsHash": hex.EncodeToString(agreementTerms.Hash()),
			"parties": []map[string]interface{}{
				{
					"type": "signatory",
					"id":   inboxAddress.String(),
				},
				{
					"type": "signatory",
					"id":   responderAddress.String(),
				},
			},
		}

		// create a credential to serve as our agreement
		// the subject of our credential will be ourselves,
		// signifying our agreement to the terms.
		// our counterparty will issue a credential in the
		// same manner.
		unsignedAgreementCredential, err := credential.NewCredential().
			CredentialType([]string{"VerifiableCredential", "AgreementCredential"}).
			CredentialSubject(credential.AddressKey(inboxAddress)).
			CredentialSubjectClaims(claims).
			CredentialSubjectClaim("terms", hex.EncodeToString(agreementTerms.Id())).
			Issuer(credential.AddressKey(inboxAddress)).
			ValidFrom(time.Now()).
			SignWith(inboxAddress, time.Now()).
			Finish()

		if err != nil {
			log.Fatal("failed to create credential", "error", err)
		}

		signedAgreementCredential, err := selfAccount.CredentialIssue(unsignedAgreementCredential)
		if err != nil {
			log.Fatal("failed to issue credential", "error", err)
		}

		unsignedAgreementPresentation, err := credential.NewPresentation().
			PresentationType([]string{"VerifiablePresentation", "AgreementPresentation"}).
			Holder(credential.AddressKey(inboxAddress)).
			CredentialAdd(signedAgreementCredential).
			Finish()

		if err != nil {
			log.Fatal("failed to create presentation", "error", err)
		}

		signedAgreementPresentation, err := selfAccount.PresentationIssue(
			unsignedAgreementPresentation,
		)

		if err != nil {
			log.Fatal("failed to issue presentation", "error", err)
		}

		// create a new request and store a reference to it
		content, err = message.NewCredentialVerificationRequest().
			Type([]string{"VerifiableCredential", "AgreementCredential"}).
			Evidence("terms", agreementTerms).
			Proof(signedAgreementPresentation).
			Expires(time.Now().Add(time.Hour * 24)).
			Finish()

		if err != nil {
			log.Fatal("failed to encode credential request message", "error", err)
		}

		verificationCompleter := make(chan *message.CredentialVerificationResponse, 1)

		requests.Store(
			hex.EncodeToString(content.ID()),
			verificationCompleter,
		)

		// send the presentation request to the responding user
		err = selfAccount.MessageSend(responderAddress, content)
		if err != nil {
			log.Fatal("failed to send credential request message", "error", err)
		}

		response := <-verificationCompleter

		log.Info("Response received with status", "status", response.Status())
		for _, c := range response.Credentials() {
			err = c.Validate()
			if err != nil {
				log.Warn("failed to validate credential", "error", err)
				continue
			}

			/*
				// check that the credential is not yet valid for use
				if c.ValidFrom().After(time.Now()) {
					log.Warn("credential is intended to be used in the future")
					continue
				}
			*/

			claims, err := c.CredentialSubjectClaims()
			if err != nil {
				log.Warn("failed to parse credential claims", "error", err)
				continue
			}

			parties, ok := claims["parties"].([]interface{})
			if !ok {
				log.Warn("parties claim is not an array")
				continue
			}

			var isIssued, isSigner bool

			for _, subject := range parties {
				subjectDetails, ok := subject.(map[string]interface{})
				if !ok {
					log.Warn("subject is not an object")
					continue
				}

				subjectType, ok := subjectDetails["type"].(string)
				if !ok || subjectType != "signatory" {
					continue
				}

				subjectID, ok := subjectDetails["id"].(string)
				if !ok {
					log.Warn("subject id is not a string")
					continue
				}

				// check if the agreement issuer (alice) is provided in the agreement
				if subjectID == inboxAddress.String() {
					isIssued = true
				}

				// check if the responder is included as a signer in the agreement
				if subjectID == responderAddress.String() {
					isSigner = true
				}
			}

			if isIssued && isSigner {
				log.Info("Agreement is valid and signed by both parties")
				selfAccount.CredentialStore(c)
			} else {
				log.Warn("Agreement is not valid or not signed by both parties")
			}
		}
		os.Exit(1)
	}
}
