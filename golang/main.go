package main

import (
	"crypto/rand"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/joinself/self-go-sdk/account"
	"github.com/joinself/self-go-sdk/event"
	"github.com/joinself/self-go-sdk/message"
)

var selfAccount *account.Account

func main() {
	fmt.Println("🔗 Self SDK Connection Server")
	fmt.Println("=============================")

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
				log.Println("✅ Connected to Self network")
			},
			OnDisconnect: func(acc *account.Account, err error) {
				log.Println("❌ Disconnected from Self network:", err)
			},
			OnWelcome: func(acc *account.Account, wlc *event.Welcome) {
				log.Printf("🎉 Connection received from: %s", wlc.FromAddress().String())

				// Accept the connection request
				_, err := acc.ConnectionAccept(wlc.ToAddress(), wlc.Welcome())
				if err != nil {
					log.Printf("❌ Failed to accept connection: %v", err)
					return
				}

				log.Println("✅ Connection established successfully!")
				log.Println("🚀 Ready to exchange messages and credentials")

				// Generate new QR code for the next connection
				fmt.Println("\n📱 Ready for next connection:")
				displayConnectionQR()
			},
			OnKeyPackage: func(acc *account.Account, kp *event.KeyPackage) {
				_, err := acc.ConnectionEstablish(kp.ToAddress(), kp.KeyPackage())
				if err != nil {
					log.Println("❌ OnKeyPackage: Failed to establish connection:", err)
				} else {
					log.Println("✅ OnKeyPackage: Successfully established connection with client:", kp.FromAddress())
				}
			},
			OnMessage: func(acc *account.Account, msg *event.Message) {
				switch event.ContentTypeOf(msg) {
				case message.ContentTypeDiscoveryRequest:
					log.Printf("📨 Received discovery request from %s", msg.FromAddress())
					handleDiscoveryRequest(acc, msg)
				case message.ContentTypeDiscoveryResponse:
					log.Printf("📨 Received discovery response from %s", msg.FromAddress())
					handleDiscoveryResponse(msg)
				case message.ContentTypeIntroduction:
					log.Printf("📨 Received introduction message from %s", msg.FromAddress())
				default:
					log.Printf("📨 Unknown message type: %d from %s", event.ContentTypeOf(msg), msg.FromAddress())
				}
			},
		},
	}

	// start self account
	selfAccount, err = account.New(cfg)
	if err != nil {
		log.Fatal("failed to initialize account: ", err)
	}

	log.Println("🔧 Self account initialized")

	// Generate initial QR code after account is ready
	fmt.Println("\n📱 Initial connection QR code:")
	displayConnectionQR()

	// handle graceful shutdown
	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		sig := <-sigs
		log.Println("🛑 Received signal:", sig)
		err := selfAccount.Close()
		if err != nil {
			log.Println("❌ Error closing SDK:", err)
		} else {
			log.Println("✅ Self client closed gracefully")
		}
		os.Exit(0)
	}()

	defer func() {
		log.Println("🧹 Cleaning up...")
		err := selfAccount.Close()
		if err != nil {
			log.Println("❌ Error in defer close:", err)
		} else {
			log.Println("✅ Self client closed")
		}
	}()

	// Keep the server running
	fmt.Println("⏳ Server running... Press Ctrl+C to exit")
	select {}
}

// displayConnectionQR generates and displays a QR code in the terminal
func displayConnectionQR() {
	qrCode, expiresAt, err := generateConnectionQR()
	if err != nil {
		log.Printf("❌ Failed to generate QR code: %v", err)
		return
	}

	fmt.Println("\n" + qrCode)
	fmt.Printf("⏱️  Expires: %s\n", expiresAt.Format("15:04:05 MST"))
	fmt.Println("📱 Scan this QR code with your Self mobile app to establish a connection")
	fmt.Println()
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
