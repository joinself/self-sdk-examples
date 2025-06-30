package main

import (
	"bytes"
	"crypto/rand"
	"encoding/hex"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/go-pdf/fpdf"
	"github.com/joinself/self-go-sdk/account"
	"github.com/joinself/self-go-sdk/credential"
	"github.com/joinself/self-go-sdk/event"
	"github.com/joinself/self-go-sdk/keypair/signing"
	"github.com/joinself/self-go-sdk/message"
	"github.com/joinself/self-go-sdk/object"
)

var inboxAddress *signing.PublicKey

func main() {
	startSelf()
}

func startSelf() {
	// clear state
	err := os.RemoveAll("./self-store")
	if err != nil {
		log.Fatalf("Failed to clear self-store: %v\n", err)
	}

	storageKey, err := generateRandomBytes(32)
	if err != nil {
		log.Fatalf("error generating random bytes: %v", err)
	}

	// configure self account and callbacks
	cfg := &account.Config{
		StoragePath: "./self-store",
		StorageKey:  storageKey,
		Environment: account.TargetSandbox,
		LogLevel:    account.LogWarn,
		Callbacks: account.Callbacks{
			OnConnect: func(selfAccount *account.Account) {
			},
			OnDisconnect: func(selfAccount *account.Account, err error) {
			},
			OnWelcome: func(selfAccount *account.Account, wlc *event.Welcome) {
			},
			OnKeyPackage: func(selfAccount *account.Account, kp *event.KeyPackage) {
				_, err := selfAccount.ConnectionEstablish(kp.ToAddress(), kp.KeyPackage())
				if err != nil {
					log.Println("OnKeyPackage: Failed to establish connection:", err)
					panic(err)
				} else {
					log.Println("OnKeyPackage: Successfully established connection with client:", kp.FromAddress())
				}
			},
			OnMessage: func(selfAccount *account.Account, msg *event.Message) {
				switch event.ContentTypeOf(msg) {
				case message.ContentTypeCredentialPresentationResponse:
					handleCredentialResponse(msg)
				case message.ContentTypeCredentialVerificationResponse:
					handleDocumentSigningResponse(msg)
				case message.ContentTypeChat:
					handleChatMessage(selfAccount, msg)
				case message.ContentTypeIntroduction:
				default:
					log.Println("OnMessage: Unknown message type:", event.ContentTypeOf(msg))
				}
			},
		},
	}

	// start self account
	selfAccount, err := account.New(cfg)
	if err != nil {
		log.Fatal("failed to initialize account: ", err)
	}

	log.Println("self account initialized")

	inboxList, err := selfAccount.InboxList()
	if err != nil {
		log.Fatal("failed to get inbox list:", err)
	}

	inboxAddress = inboxList[0]

	log.Println("server address:", inboxList[0])

	// handle graceful shutdown
	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		sig := <-sigs
		log.Println("received signal:", sig)
		err := selfAccount.Close()
		if err != nil {
			log.Println("error closing SDK:", err)
		} else {
			log.Println("self client closed gracefully")
		}
		os.Exit(0)
	}()

	defer func() {
		log.Println("defer: closing SDK")
		err := selfAccount.Close()
		if err != nil {
			log.Println("error in defer close:", err)
		} else {
			log.Println("self client closed in defer")
		}
	}()

	// keep the server running
	select {}
}

func handleChatMessage(selfAccount *account.Account, msg *event.Message) {
	chatMessage, err := message.DecodeChat(msg.Content())
	if err != nil {
		log.Printf("HandleChatMessage: Failed to decode chat message from %s: %v", msg.FromAddress(), err)
		return
	}

	command := chatMessage.Message()

	switch command {
	case "REQUEST_CREDENTIAL_AUTH":
		sendCredentialRequest(selfAccount, msg, "liveness")
	case "PROVIDE_CREDENTIAL_EMAIL":
		sendCredentialRequest(selfAccount, msg, "email")
	case "PROVIDE_CREDENTIAL_DOCUMENT":
		sendCredentialRequest(selfAccount, msg, "document")
	case "PROVIDE_CREDENTIAL_CUSTOM":
		sendCredentialRequest(selfAccount, msg, "custom")
	case "REQUEST_GET_CUSTOM_CREDENTIAL":
		sendCustomCredential(selfAccount, msg)
	case "REQUEST_DOCUMENT_SIGNING":
		sendDocumentSigningRequest(selfAccount, msg)
	default:
		log.Printf("HandleChatMessage: Unknown command '%s' from %s - ignoring", command, msg.FromAddress())
	}
}

func sendCredentialRequest(selfAccount *account.Account, msg *event.Message, credentialType string) {
	var content *message.Content
	var err error

	switch credentialType {
	case "liveness":
		content, err = message.NewCredentialPresentationRequest().
			Type([]string{"VerifiablePresentation", "CustomPresentation"}).
			Details(
				credential.CredentialTypeLiveness,
				[]*message.CredentialPresentationDetailParameter{
					message.NewCredentialPresentationDetailParameter(
						message.OperatorNotEquals,
						"sourceImageHash",
						"",
					),
				},
			).
			Finish()

	case "email":
		content, err = message.NewCredentialPresentationRequest().
			Type([]string{"VerifiablePresentation", "CustomPresentation"}).
			Details(
				credential.CredentialTypeEmail,
				[]*message.CredentialPresentationDetailParameter{
					message.NewCredentialPresentationDetailParameter(
						message.OperatorNotEquals,
						"emailAddress",
						"",
					),
				},
			).
			Finish()

	case "document":
		content, err = message.NewCredentialPresentationRequest().
			Type([]string{"VerifiablePresentation", "CustomPresentation"}).
			Details(
				credential.CredentialTypePassport,
				[]*message.CredentialPresentationDetailParameter{
					message.NewCredentialPresentationDetailParameter(
						message.OperatorNotEquals,
						"documentNumber",
						"",
					),
				},
			).
			Finish()

	case "custom":
		content, err = message.NewCredentialPresentationRequest().
			Type([]string{"VerifiablePresentation", "CustomPresentation"}).
			Details(
				[]string{"VerifiableCredential", "CustomerCredential"},
				[]*message.CredentialPresentationDetailParameter{
					message.NewCredentialPresentationDetailParameter(
						message.OperatorNotEquals,
						"name",
						"",
					),
				},
			).
			Finish()

	default:
		log.Printf("sendCredentialRequest: Unsupported credential type '%s' from %s", credentialType, msg.FromAddress())
		return
	}

	if err != nil {
		log.Fatalf("SendCredentialRequest: Failed to build %s credential request: %v", credentialType, err)
	}

	err = selfAccount.MessageSend(msg.FromAddress(), content)
	if err != nil {
		log.Fatalf("SendCredentialRequest: Failed to send %s request message to %s: %v", credentialType, msg.FromAddress(), err)
	} else {
		log.Printf("SendCredentialRequest: Sent %s credential request to: %s", credentialType, msg.FromAddress())
	}
}

func sendCustomCredential(selfAccount *account.Account, msg *event.Message) {
	credentialType := []string{"VerifiableCredential", "CustomerCredential"}
	subjectAddress := credential.AddressKey(msg.FromAddress())
	issuerAddress := credential.AddressKey(inboxAddress)

	customerCredential, err := credential.NewCredential().
		CredentialType(credentialType).
		CredentialSubject(subjectAddress).
		CredentialSubjectClaims(map[string]any{
			"name": "Test Name",
		}).
		Issuer(issuerAddress).
		ValidFrom(time.Now()).
		SignWith(inboxAddress, time.Now()).
		Finish()

	if err != nil {
		log.Fatal("sendCustomCredential: failed to build credential", "error", err)
	}

	customerVerifiableCredential, err := selfAccount.CredentialIssue(customerCredential)
	if err != nil {
		log.Fatal("sendCustomCredential: failed to build credential", "error", err)
	}

	content, err := message.NewCredential().
		VerifiableCredential(customerVerifiableCredential).
		Finish()

	if err != nil {
		log.Fatal("sendCustomCredential: failed to encode credential request message", "error", err)
	}

	err = selfAccount.MessageSend(msg.FromAddress(), content)
	if err != nil {
		log.Fatal("sendCustomCredential: failed to send credential message", "error", err)
	} else {
		log.Printf("sendCustomCredential: custom credential sent to %s", msg.FromAddress())
	}
}

func handleCredentialResponse(msg *event.Message) {
	response, err := message.DecodeCredentialPresentationResponse(msg.Content())
	if err != nil {
		log.Println("handleCredentialResponse: Failed to decode credential response from", msg.FromAddress(), "error:", err)
		return
	}

	var content string

	for i, p := range response.Presentations() {
		err = p.Validate()
		if err != nil {
			log.Printf("handleCredentialResponse: VALIDATION FAILED for presentation %d: %v", i+1, err)
			continue
		}

		if !p.Holder().Address().Matches(msg.FromAddress()) {
			log.Printf("handleCredentialResponse: SECURITY WARNING - presentation holder address mismatch. Expected: %s, Got: %s",
				msg.FromAddress(), p.Holder().Address())
			continue
		}

		for _, credential := range p.Credentials() {
			err = credential.Validate()
			if err != nil {
				log.Printf("handleCredentialResponse: CREDENTIAL VALIDATION FAILED: %v", err)
				continue
			}

			if credential.ValidFrom().After(time.Now()) {
				log.Printf("handleCredentialResponse: WARNING - credential is not yet valid (valid from: %v, current time: %v)",
					credential.ValidFrom(), time.Now())
				continue
			}

			claims, err := credential.CredentialSubjectClaims()
			if err != nil {
				log.Println(err)
				return
			}

			for k, v := range claims {
				if k == "sourceImageHash" {
					content = content + "Authentication = true"
					continue
				}
				if k != "id" && k != "sourceImageHash" && k != "targetImageHash" {
					content = content + k + " = " + v.(string)
					continue
				}
			}
		}
	}

	if response.Status().String() == "Accepted" && content != "" {
	} else if response.Status().String() == "Accepted" && content == "" {
		content = content + "content empty"
	} else {
		content = content + "request rejected"
	}

	log.Println("handleCredentialResponse: " + content)
}

func sendDocumentSigningRequest(selfAccount *account.Account, msg *event.Message) {
	serverAddress := inboxAddress
	clientAddress := msg.FromAddress()

	pdf := fpdf.New("P", "mm", "A4", "")
	pdf.AddPage()
	pdf.SetFont("Arial", "B", 16)
	pdf.Cell(40, 10, "Document Signing Agreement")
	pdf.Ln(20)
	pdf.SetFont("Arial", "", 12)
	pdf.Cell(0, 10, "This document represents an agreement between:")
	pdf.Ln(10)
	pdf.Cell(0, 10, "Server: "+serverAddress.String())
	pdf.Ln(10)
	pdf.Cell(0, 10, "Client: "+clientAddress.String())
	pdf.Ln(10)
	pdf.Cell(0, 10, "By signing this agreement, both parties acknowledge")
	pdf.Ln(10)
	pdf.Cell(0, 10, "the terms and conditions of this document signing process.")

	agreementBuf := bytes.NewBuffer(make([]byte, 1024))
	err := pdf.Output(agreementBuf)
	if err != nil {
		log.Fatalf("SendDocumentSigningRequest: Failed to generate PDF: %v", err)
	}

	agreementTerms, err := object.New("application/pdf", agreementBuf.Bytes())
	if err != nil {
		log.Fatalf("SendDocumentSigningRequest: Failed to create agreement object: %v", err)
	}

	err = selfAccount.ObjectUpload(agreementTerms, false)
	if err != nil {
		log.Fatalf("SendDocumentSigningRequest: Failed to upload agreement object: %v", err)
	}

	claims := map[string]interface{}{
		"termsHash": hex.EncodeToString(agreementTerms.Hash()),
		"parties": []map[string]interface{}{
			{"type": "signatory", "id": serverAddress.String()},
			{"type": "signatory", "id": clientAddress.String()},
		},
	}

	unsignedAgreementCredential, err := credential.NewCredential().
		CredentialType([]string{"VerifiableCredential", "AgreementCredential"}).
		CredentialSubject(credential.AddressKey(serverAddress)).
		CredentialSubjectClaims(claims).
		CredentialSubjectClaim("terms", hex.EncodeToString(agreementTerms.Id())).
		Issuer(credential.AddressKey(serverAddress)).
		ValidFrom(time.Now()).
		SignWith(serverAddress, time.Now()).
		Finish()

	if err != nil {
		log.Fatalf("SendDocumentSigningRequest: Failed to create credential: %v", err)
	}

	signedAgreementCredential, err := selfAccount.CredentialIssue(unsignedAgreementCredential)
	if err != nil {
		log.Fatalf("SendDocumentSigningRequest: Failed to issue credential: %v", err)
	}

	unsignedAgreementPresentation, err := credential.NewPresentation().
		PresentationType([]string{"VerifiablePresentation", "AgreementPresentation"}).
		Holder(credential.AddressKey(serverAddress)).
		CredentialAdd(signedAgreementCredential).
		Finish()

	if err != nil {
		log.Fatalf("SendDocumentSigningRequest: Failed to create presentation: %v", err)
	}

	signedAgreementPresentation, err := selfAccount.PresentationIssue(unsignedAgreementPresentation)
	if err != nil {
		log.Fatalf("SendDocumentSigningRequest: Failed to issue presentation: %v", err)
	}

	content, err := message.NewCredentialVerificationRequest().
		Type([]string{"VerifiableCredential", "AgreementCredential"}).
		Evidence("terms", agreementTerms).
		Proof(signedAgreementPresentation).
		Expires(time.Now().Add(time.Hour * 24)).
		Finish()

	if err != nil {
		log.Fatalf("SendDocumentSigningRequest: Failed to build verification request: %v", err)
	}

	err = selfAccount.MessageSend(msg.FromAddress(), content)
	if err != nil {
		log.Fatalf("SendDocumentSigningRequest: Failed to send document signing request to %s: %v", msg.FromAddress(), err)
	} else {
		log.Printf("SendDocumentSigningRequest: Successfully sent document signing request to: %s", msg.FromAddress())
	}
}

func handleDocumentSigningResponse(msg *event.Message) {
	response, err := message.DecodeCredentialVerificationResponse(msg.Content())
	if err != nil {
		log.Printf("handleDocumentSigningResponse: Failed to decode verification response from %s: %v", msg.FromAddress(), err)
		return
	}

	switch response.Status() {
	case message.ResponseStatusAccepted, message.ResponseStatusCreated:
		log.Printf("handleDocumentSigningResponse: Client %s has digitally signed the agreement", msg.FromAddress())
	case message.ResponseStatusUnauthorized, message.ResponseStatusForbidden, message.ResponseStatusNotAcceptable:
		log.Printf("handleDocumentSigningResponse: Client %s declined to sign the agreement", msg.FromAddress())
	default:
		log.Printf("handleDocumentSigningResponse: UNKNOWN RESPONSE STATUS from client: %s", msg.FromAddress())
	}
}

func generateRandomBytes(size int) ([]byte, error) {
	b := make([]byte, size)
	_, err := rand.Read(b)
	if err != nil {
		return nil, err
	}
	return b, nil
}
