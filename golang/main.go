package main

import (
	"bytes"
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/go-pdf/fpdf"
	"github.com/joinself/self-go-sdk/account"
	"github.com/joinself/self-go-sdk/credential"
	"github.com/joinself/self-go-sdk/credential/predicate"
	"github.com/joinself/self-go-sdk/event"
	"github.com/joinself/self-go-sdk/keypair/signing"
	"github.com/joinself/self-go-sdk/message"
	"github.com/joinself/self-go-sdk/object"
)

var selfAccount *account.Account
var inboxAddress *signing.PublicKey

func main() {
	log.Println("Self SDK Connection Server")
	log.Println("=============================")

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
			OnConnect: func(acc *account.Account) {
				log.Println("Connected to Self network")
			},
			OnDisconnect: func(acc *account.Account, err error) {
				log.Println("Disconnected from Self network:", err)
			},
			OnWelcome: func(acc *account.Account, wlc *event.Welcome) {
				log.Printf("Connection received from: %s", wlc.FromAddress().String())

				// Accept the connection request
				_, err := acc.ConnectionAccept(wlc.ToAddress(), wlc.Welcome())
				if err != nil {
					log.Printf("Failed to accept connection: %v", err)
					return
				}

				log.Println("Connection established successfully!")
				log.Println("Ready to exchange messages and credentials")

				// Generate new QR code for the next connection
				log.Println("\nReady for next connection:")
				displayConnectionQR()
			},
			OnKeyPackage: func(acc *account.Account, kp *event.KeyPackage) {
				_, err := acc.ConnectionEstablish(kp.ToAddress(), kp.KeyPackage())
				if err != nil {
					log.Println("OnKeyPackage: Failed to establish connection:", err)
					panic(err)
				} else {
					log.Println("OnKeyPackage: Successfully established connection with client:", kp.FromAddress())
				}
			},
			OnMessage: func(acc *account.Account, msg *event.Message) {
				contentType := event.ContentTypeOf(msg)
				if contentType == message.ContentTypeCredentialPresentationResponse {
					handleCredentialResponse(msg)
				} else if contentType == message.ContentTypeCredentialVerificationResponse {
					handleDocumentSigningResponse(msg)
				} else if contentType == message.ContentTypeChat {
					handleChatMessage(acc, msg)
				} else if contentType == message.ContentTypeDiscoveryRequest {
					log.Printf("Received discovery request from %s", msg.FromAddress())
					handleDiscoveryRequest(acc, msg)
				} else if contentType == message.ContentTypeDiscoveryResponse {
					log.Printf("Received discovery response from %s", msg.FromAddress())
					handleDiscoveryResponse(msg)
				} else if contentType == message.ContentTypeIntroduction {
					log.Printf("Received introduction message from %s", msg.FromAddress())
				} else {
					log.Printf("Unknown message type: %d from %s", event.ContentTypeOf(msg), msg.FromAddress())
				}
			},
		},
	}

	// start self account
	selfAccount, err = account.New(cfg)
	if err != nil {
		log.Fatal("failed to initialize account: ", err)
	}

	log.Println("Self account initialized")

	inboxList, err := selfAccount.InboxList()
	if err != nil {
		log.Fatal("failed to get inbox list:", err)
	}

	inboxAddress = inboxList[0]

	log.Println("server address:", inboxList[0])

	// Generate initial QR code after account is ready
	log.Println("\nInitial connection QR code:")
	displayConnectionQR()

	// handle graceful shutdown
	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		sig := <-sigs
		log.Println("Received signal:", sig)
		err := selfAccount.Close()
		if err != nil {
			log.Println("Error closing SDK:", err)
		} else {
			log.Println("Self client closed gracefully")
		}
		os.Exit(0)
	}()

	defer func() {
		log.Println("Cleaning up...")
		err := selfAccount.Close()
		if err != nil {
			log.Println("Error in defer close:", err)
		} else {
			log.Println("Self client closed")
		}
	}()

	// Keep the server running
	log.Println("Server running... Press Ctrl+C to exit")
	select {}
}

// displayConnectionQR generates and displays a QR code in the terminal
func displayConnectionQR() {
	qrCode, expiresAt, err := generateConnectionQR()
	if err != nil {
		log.Printf("Failed to generate QR code: %v", err)
		return
	}

	log.Println("\n" + qrCode)
	log.Printf("Expires: %s\n", expiresAt.Format("15:04:05 MST"))
	log.Println("Scan this QR code with your Self mobile app to establish a connection")
	log.Println()
}

// generateConnectionQR creates a QR code for mobile app connections
func generateConnectionQR() (string, time.Time, error) {
	// Open inbox for receiving connection requests
	currentInboxAddress, err := selfAccount.InboxOpen()
	if err != nil {
		log.Printf("generateConnectionQR: Failed to open inbox: %v", err)
		return "", time.Time{}, fmt.Errorf("failed to open inbox: %v", err)
	}

	// Generate cryptographic key package for secure communication
	expirationTime := time.Now().Add(30 * time.Minute)
	keyPackage, err := selfAccount.ConnectionNegotiateOutOfBand(
		currentInboxAddress,
		expirationTime,
	)
	if err != nil {
		log.Printf("generateConnectionQR: Failed to generate key package: %v", err)
		return "", time.Time{}, fmt.Errorf("failed to generate key package: %v", err)
	}

	// Build discovery request message
	content, err := message.NewDiscoveryRequest().
		KeyPackage(keyPackage).
		Expires(expirationTime).
		Finish()
	if err != nil {
		log.Printf("generateConnectionQR: Failed to build discovery request: %v", err)
		return "", time.Time{}, fmt.Errorf("failed to build discovery request: %v", err)
	}

	// Create anonymous message and encode to QR (Unicode for terminal display)
	anonymousMsg := event.NewAnonymousMessage(content)
	anonymousMsg.SetFlags(event.MessageFlagTargetSandbox)

	qrCode, err := anonymousMsg.EncodeToQR(event.QREncodingUnicode)
	if err != nil {
		log.Printf("generateConnectionQR: Failed to generate QR code: %v", err)
		return "", time.Time{}, fmt.Errorf("failed to generate QR code: %v", err)
	}

	return string(qrCode), expirationTime, nil
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
			PresentationType("CustomPresentation").
			Predicates(
				predicate.NewTree(
					predicate.Contains(
						credential.FieldType,
						credential.CredentialTypeLiveness,
					).And(
						predicate.NotEmpty(
							credential.FieldSubjectLivenessSourceImageHash,
						),
					),
				),
			).
			Finish()

	case "email":
		content, err = message.NewCredentialPresentationRequest().
			PresentationType("CustomPresentation").
			Predicates(
				predicate.NewTree(
					predicate.Contains(
						credential.FieldType,
						credential.CredentialTypeEmail,
					).And(
						predicate.NotEmpty(
							credential.FieldSubjectEmailAddress,
						),
					),
				),
			).
			Finish()

	case "document":
		content, err = message.NewCredentialPresentationRequest().
			PresentationType("CustomPresentation").
			Predicates(
				predicate.NewTree(
					predicate.Contains(
						credential.FieldType,
						credential.CredentialTypePassport,
					).And(
						predicate.NotEmpty(
							credential.FieldSubjectPassportDocumentNumber,
						),
					),
				),
			).
			Finish()

	case "custom":
		content, err = message.NewCredentialPresentationRequest().
			PresentationType("CustomPresentation").
			Predicates(
				predicate.NewTree(
					predicate.Contains(
						credential.FieldType,
						"CustomerCredential",
					).And(
						predicate.NotEmpty(
							"/credentialSubject/name",
						),
					),
				),
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
	credentialType := "CustomerCredential"
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
		CredentialType("AgreementCredential").
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
		PresentationType("AgreementPresentation").
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
		Type("AgreementCredential").
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

	status := response.Status()
	if status == message.ResponseStatusAccepted || status == message.ResponseStatusCreated {
		log.Printf("handleDocumentSigningResponse: Client %s has digitally signed the agreement", msg.FromAddress())
	} else if status == message.ResponseStatusUnauthorized || status == message.ResponseStatusForbidden || status == message.ResponseStatusNotAcceptable {
		log.Printf("handleDocumentSigningResponse: Client %s declined to sign the agreement", msg.FromAddress())
	} else {
		log.Printf("handleDocumentSigningResponse: UNKNOWN RESPONSE STATUS from client: %s", msg.FromAddress())
	}
}

func handleDiscoveryRequest(selfAccount *account.Account, msg *event.Message) {
	log.Printf("handleDiscoveryRequest: Processing discovery request from %s", msg.FromAddress())

	// Send a discovery response accepting the request
	content, err := message.NewDiscoveryResponse().
		ResponseTo(msg.Content().ID()).
		Status(message.ResponseStatusAccepted).
		Finish()

	if err != nil {
		log.Printf("handleDiscoveryRequest: Failed to build discovery response: %v", err)
		return
	}

	err = selfAccount.MessageSend(msg.FromAddress(), content)
	if err != nil {
		log.Printf("handleDiscoveryRequest: Failed to send discovery response to %s: %v", msg.FromAddress(), err)
	} else {
		log.Printf("handleDiscoveryRequest: Successfully sent discovery response to %s", msg.FromAddress())
	}
}

func handleDiscoveryResponse(msg *event.Message) {
	discoveryResponse, err := message.DecodeDiscoveryResponse(msg.Content())
	if err != nil {
		log.Printf("handleDiscoveryResponse: Failed to decode discovery response from %s: %v", msg.FromAddress(), err)
		return
	}

	log.Printf("handleDiscoveryResponse: Received discovery response from %s with status: %s",
		msg.FromAddress(), discoveryResponse.Status().String())
}

func generateRandomBytes(size int) ([]byte, error) {
	b := make([]byte, size)
	_, err := rand.Read(b)
	if err != nil {
		return nil, err
	}
	return b, nil
}
