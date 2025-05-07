package com.joinself;


import com.joinself.selfsdk.kmp.account.*;
import com.joinself.selfsdk.kmp.error.SelfStatus;
import com.joinself.selfsdk.kmp.error.SelfStatusName;
import com.joinself.selfsdk.kmp.event.*;
import com.joinself.selfsdk.kmp.keypair.signing.PublicKey;
import com.joinself.selfsdk.kmp.message.*;
import com.joinself.selfsdk.kmp.time.Timestamp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class Main {
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xff & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);

        }
        return hexString.toString();
    }


    static PublicKey inboxAddress = null;
    static PublicKey responderAddress = null;
    static String discoveryRequestId = "";

    public static void main(String[] args) {
        System.out.println("Chat example in Java");
        String javaVersion = System.getProperty("java.version");
        System.out.println("Java version: " + javaVersion);

        try {
            Semaphore signal = new Semaphore(1);
            signal.acquire();

            String storagePath = ":memory:";
            byte[] storageKey = new byte[32];
            Account account = new Account();
            SelfStatus status = account.configure(
                    storagePath,
                    storageKey,
                    Target.PREVIEW_SANDBOX.rpcEndpoint(),
                    Target.PREVIEW_SANDBOX.objectEndpoint(),
                    Target.PREVIEW_SANDBOX.messageEndpoint(),
                    LogLevel.INFO,
                    new ConfigureCallbacks() {
                        @Override
                        public void onDisconnect(@Nullable SelfStatus reason) {
                            System.out.println("KMP Disconnected with reason: " + reason);
                        }

                        @Override
                        public void onConnect() {
                            System.out.println("KMP connected");
                            signal.release();
                        }

                        @Override
                        public void onAcknowledgement(@NotNull Reference reference) {
                            System.out.println("KMP onAcknowledgement");
                        }

                        @Override
                        public void onError(@NotNull Reference reference, @NotNull SelfStatus selfStatus) {
                            System.out.println("KMP onError");
                        }

                        @Override
                        public void onProposal(@Nullable Proposal proposal) {
                            System.out.println("KMP proposal");
                        }

                        @Override
                        public void onKeyPackage(@Nullable KeyPackage keyPackage) {
                            System.out.println("KMP keypackage");
                        }

                        @Override
                        public void onCommit(@Nullable Commit commit) {
                            System.out.println("KMP commited");
                        }

                        @Override
                        public void onWelcome(@Nullable Welcome welcome) {
                            System.out.println("KMP welcome");
                            account.connectionAccept(welcome.toAddress(), welcome.welcome(), new ConnectionCallback() {
                                @Override
                                public void onCompletion(@Nullable SelfStatus status, @NotNull PublicKey groupAddress) {
                                    System.out.println("accepted connection encrypted group status:" + SelfStatusName.Companion.getName(status.code()) + " - from:" + welcome.fromAddress().encodeHex() + " - group:" + groupAddress.encodeHex());
                                }
                            });
                        }

                        @Override
                        public void onMessage(@Nullable Message message) {
                            Content content = message.content();
                            ContentType contentType = content.contentType();
                            System.out.println("received message" + contentType.name());
                            switch (contentType) {
                                case DISCOVERY_RESPONSE -> {
                                    DiscoveryResponse discoveryResponse = DiscoveryResponse.Companion.decode(content);
                                    String responseTo = bytesToHex(discoveryResponse.responseTo());
                                    System.out.println("received response to discovery request from:" + message.fromAddress().encodeHex() + " - requestId:" + responseTo + " - messageId:" + bytesToHex(message.id()));
                                    if (responseTo != discoveryRequestId) {
                                        System.out.println("received response to unknown request requestId:" + responseTo);
                                    }
                                    responderAddress = message.fromAddress();

                                    signal.release();
                                }
                                case CHAT -> {
                                    Chat chat = Chat.Companion.decode(content);
                                    System.out.println(
                                            "received chat message " +
                                                    "\nfrom:"  + message.fromAddress().encodeHex() +
                                                    "\nmessageId:" + bytesToHex(message.id()) +
                                                    "\nmessage:" + chat.message() +
                                                    "\nattachments:" + chat.attachments().length
                                    );
                                    System.out.println("\n");
                                }
                            }
                        }
                    }
            );
            signal.acquire();
            System.out.println("status:" + SelfStatusName.Companion.getName(status.code()));
            account.inboxOpen(Timestamp.Companion.now() + 360, new InboxOpenCallback() {
                @Override
                public void onCompletion(@Nullable SelfStatus selfStatus, @Nullable PublicKey address) {
                    System.out.println("inbox open status:" + SelfStatusName.Companion.getName(status.code()) + " - address:" + address.encodeHex());
                    inboxAddress = address;
                    signal.release();
                }
            });
            signal.acquire();

            if (inboxAddress == null) {
                throw new Exception("Can't open inbox");
            }

            long expires = Timestamp.Companion.now() + 3600;
            com.joinself.selfsdk.kmp.crypto.KeyPackage keyPackage = account.connectionNegotiateOutOfBand(inboxAddress, expires);
            Content discoveryRequest = new DiscoveryRequestBuilder()
                    .keyPackage(keyPackage)
                    .expires(expires)
                    .finish();

            AnonymousMessage anonymousMessage = AnonymousMessage.Companion.fromContent(discoveryRequest);
            anonymousMessage.setFlags(new FlagSet(Flag.TARGET_SANDBOX));
            byte[] qrCodeBytes = anonymousMessage.encodeQR(QrEncoding.UNICODE);
            String qrCodeString = new String(qrCodeBytes, StandardCharsets.UTF_8);
            System.out.println("scan the qr code to complete the discovery request");
            System.out.println(qrCodeString);

            discoveryRequestId = bytesToHex(discoveryRequest.id());
            System.out.println("waiting for response to discovery request requestId:" + discoveryRequestId);
            signal.acquire();

            if (responderAddress == null) {
                System.out.println("responder address is null");
                return;
            }
            System.out.println("\n");

            Content chat = new ChatBuilder()
                    .message("hello")
                    .finish();
            account.messageSend(responderAddress, chat);
            signal.acquire();

            String msgId = bytesToHex(chat.id());
            System.out.println("send chat status:" + " - to:" + responderAddress.encodeHex() + " - messageId:" + msgId);

            Scanner scanner = new Scanner(System.in);
            System.out.println("\n");
            System.out.println("Press enter to exit");
            scanner.nextLine();
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}