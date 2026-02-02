package com.joinself

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.joinself.protocol.rpc.developer.AdminApplicationSetupCompletion
import com.joinself.protocol.rpc.developer.AdminApplicationSetupRequest
import com.joinself.protocol.rpc.developer.AdminApplicationSetupResponse
import com.joinself.protocol.rpc.developer.Content
import com.joinself.protocol.rpc.developer.ControllerIdentityCreationApprovalRequest
import com.joinself.protocol.rpc.developer.ControllerIdentityCreationApprovalResponse
import com.joinself.protocol.rpc.developer.ControllerPairwiseIdentityNegotiationRequest
import com.joinself.protocol.rpc.developer.ControllerPairwiseIdentityNegotiationResponse
import com.joinself.protocol.rpc.developer.Event
import com.joinself.protocol.rpc.developer.Header
import com.joinself.protocol.rpc.developer.IdentityDetailsConfirmationRequest
import com.joinself.protocol.rpc.developer.IdentityDetailsConfirmationResponse
import com.joinself.protocol.rpc.developer.RequestHeader
import com.joinself.protocol.rpc.developer.Version
import com.joinself.selfsdk.account.*
import com.joinself.selfsdk.account.Target
import com.joinself.selfsdk.credential.Address
import com.joinself.selfsdk.credential.CredentialField
import com.joinself.selfsdk.credential.CredentialType
import com.joinself.selfsdk.credential.PresentationBuilder
import com.joinself.selfsdk.credential.PresentationType
import com.joinself.selfsdk.credential.VerifiablePresentation
import com.joinself.selfsdk.credential.predicate.Predicate
import com.joinself.selfsdk.credential.predicate.PredicateTree
import com.joinself.selfsdk.error.SelfStatus
import com.joinself.selfsdk.event.*
import com.joinself.selfsdk.identity.Document
import com.joinself.selfsdk.identity.Method
import com.joinself.selfsdk.identity.Operation
import com.joinself.selfsdk.identity.OperationBuilder
import com.joinself.selfsdk.identity.Role
import com.joinself.selfsdk.identity.RoleSet
import com.joinself.selfsdk.keypair.signing.PublicKey
import com.joinself.selfsdk.message.ContentType
import com.joinself.selfsdk.message.CredentialPresentationRequestBuilder
import com.joinself.selfsdk.message.CredentialPresentationResponse
import com.joinself.selfsdk.message.CredentialVerificationRequest
import com.joinself.selfsdk.message.CredentialVerificationRequestBuilder
import com.joinself.selfsdk.message.Custom
import com.joinself.selfsdk.message.CustomBuilder
import com.joinself.selfsdk.message.DiscoveryRequestBuilder
import com.joinself.selfsdk.message.DiscoveryResponse
import com.joinself.selfsdk.platform.Attestation
import com.joinself.selfsdk.time.Timestamp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okio.ByteString
import okio.ByteString.Companion.toByteString
import kotlin.coroutines.suspendCoroutine
import kotlin.io.encoding.Base64
import kotlin.random.Random


/**
 * run in terminal: ./gradlew :admin:run
 */
class AdminApp {
    var account: Account? = null
    var inboxAddress: PublicKey? = null
    var responderAddress: PublicKey? = null
    var groupAddress: PublicKey? = null
    var issuerAddress: PublicKey = PublicKey.decodeHex("004e4a0b98c5f6fe69847bdad91bc099992dfff3ac98339c7fcc78e277ff9c1213")

    val coroutineScope = CoroutineScope(Dispatchers.IO)
    val onConnect: Channel<Boolean> = Channel()
    var onReceivedWelcomeCallback: ((welcome: Welcome) -> Unit)? = null

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun run() {
        println("Admin Sample")

        val config = Config(
            storagePath = ":memory:",
            storageKey = ByteArray(size = 32),
            target = Target.preview(),
            logLevel =  LogLevel.INFO
        )
        val callbacks = Callbacks(
            onConnect = {
                println("KMP connected")
                onConnect.trySend(true)
            },
            onDisconnect = { account, reason: SelfStatus? ->
                println("KMP disconnected")
            },
            onAcknowledgement = {account, reference: Reference ->
                println("KMP onAcknowledgement id:${reference.id().toHexString()}")
            },
            onError = {account, reference: Reference, error: SelfStatus ->
                println("KMP onError")
            },
            onCommit = {account, commit: Commit ->
                println("KMP commited")
            },
            onKeyPackage = { account, keyPackage: KeyPackage ->
                account.connectionEstablish(asAddress =  keyPackage.toAddress(), keyPackage = keyPackage.keyPackage(),
                    onCompletion = {status: SelfStatus, gAddress: PublicKey ->
                        println("connection establish status:${status.name()} - group:${gAddress.encodeHex()}")
                        responderAddress = keyPackage.fromAddress()
                        groupAddress = gAddress
                    }
                )
            },
            onMessage = {account, message: Message ->
                val content = message.content()
                val contentType = content.contentType()
                println("KMP message type: $contentType")
                when (contentType) {
                    ContentType.CUSTOM -> {
                        coroutineScope.launch {
                            val custom = Custom.decode(content)
                            val event = Event.ADAPTER.decode(custom.payload())
                            if (event.content?.admin_application_setup_response != null) {
                                handleAdminSetupResponse(event.content.admin_application_setup_response)
                            }
                            if (event.content?.controller_pairwise_identity_negotiation_response != null) {
                                receivedControllerPairwiseResponse(event.content.controller_pairwise_identity_negotiation_response)
                            }
                            if (event.content?.identity_details_confirmation_response != null) {
                                confirmOrganisationDetailsResponse(event.content.identity_details_confirmation_response)
                            }
                            if (event.content?.controller_identity_creation_approval_response != null) {
                                confirmControllerApprovalResponse(event.content.controller_identity_creation_approval_response)
                            }
                        }
                    }
                    ContentType.DISCOVERY_RESPONSE -> {
                        val discoveryResponse = DiscoveryResponse.decode(content)
                        val responseTo = discoveryResponse.responseTo().toHexString()
                        println("received response to discovery request from:${message.fromAddress().encodeHex()} - requestId:${responseTo} - messageId:${message.id().toHexString()}")
                    }
                    ContentType.CREDENTIAL_PRESENTATION_RESPONSE -> {
                        val credentialResponse = CredentialPresentationResponse.decode(content)
                        credentialResponse.presentations().forEach { presentation ->
                            println("presentation type:${presentation.presentationType().toList()}")
                            presentation.credentials().forEach { credential ->
                                println("${credential.credentialType().toList()}" +
                                        "\n${credential.credentialSubjectClaims()}")
                            }
                        }
                    }
                    else -> {

                    }
                }
            },
            onProposal = {account, proposal: Proposal ->
                println("KMP proposal")
            },
            onWelcome = {account, welcome: Welcome ->
                println("KMP welcome")
                if (onReceivedWelcomeCallback != null) {
                    onReceivedWelcomeCallback!!.invoke(welcome)
                    onReceivedWelcomeCallback = null
                } else {
                    account.connectionAccept(asAddress = welcome.toAddress(), welcome = welcome.welcome()) { status: SelfStatus, gAddress: PublicKey ->
                        println("accepted connection encrypted group status:${status.name()} - from:${welcome.fromAddress().encodeHex()} - group:${gAddress.encodeHex()}")
                        responderAddress = welcome.fromAddress()
                        groupAddress = gAddress
                    }
                }
            },
            onDropped = {account, dropped: Dropped ->
                println("KMP dropped ${dropped.reason()}")
            },
            onIntegrity = {account, integrity: Integrity ->
                println("KMP integrity")
                Attestation.deviceCheck(applicationAddress = PublicKey.decodeHex("0016fced9deea88223b7faaee3e28f0363c99974c67ee7842ead128a0f36a9f1e3"), integrityToken =  ByteArray(integrity.requestHash().size + 128))
            }
        )
        account = Account(config, callbacks)
        onConnect.receive()

//        inboxOpen(account!!)

        // wait for command
        while (true) {
            val line = readln()
            if (line.isEmpty()) break
            when (line) {
                "0" -> {
                    generateQrCode(account!!)
                }
                "1" -> {
                    adminSetupRequest()
                }
                "2" -> {
                    controllerPairwiseRequest()
                }
                "3" -> {
                    confirmOrganisationDetailsRequest()
                }
                "4" -> {
                    confirmControllerApprovalRequest()
                }
                else -> {
                    println("unknown command")
                }
            }
            println("\n")
        }
    }

    var controllerIdentifier: PublicKey? = null
    var controllerInvocation: PublicKey? = null
    var organisationIdentifier: PublicKey? = null

    suspend fun adminSetupRequest() {
        inboxAddress = inboxOpen(account!!)

        val event = Event(
            header_ = Header(version = Version.V1),
            content = Content(
                admin_application_setup_request = AdminApplicationSetupRequest(inbox_address = inboxAddress!!.encodeBytes().toByteString())
            )
        )
        val encodedEvent = event.encode()
        val eventBase64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).encode(encodedEvent)

        printQRCode(eventBase64)
    }

    var adminApplicationPresentation: VerifiablePresentation? = null
    suspend fun handleAdminSetupResponse(response: AdminApplicationSetupResponse) {
        println("received AdminApplicationSetupResponse")
        val adminIdentifier = PublicKey.decodeBytes(response.document_address.toByteArray())
        val decodedOperation = Operation.decodeBytes(adminIdentifier,response.operation.toByteArray())
        suspendCancellableCoroutine { continuation ->
            account?.identityExecute(decodedOperation) {status ->
                println("identityExecute status:${status.name()} ${status.errorMessage()}")
                continuation.resumeWith(Result.success(status))
            }
        }

        val decodedPresentation = VerifiablePresentation.decodeBytes(response.presentations.first().toByteArray())
        println("presentation type:${decodedPresentation.presentationType().toList()}")
        decodedPresentation.credentials().forEach { credential ->
            println("${credential.credentialType().toList()}" +
                    "\n${credential.credentialSubjectClaims()}")
        }
        account?.presentationStore(decodedPresentation)
        adminApplicationPresentation = decodedPresentation

        adminSetupCompletion(adminIdentifier)

    }
    suspend fun adminSetupCompletion(adminIdentifier: PublicKey) {
        val event = Event(
            header_ = Header(version = Version.V1),
            content = Content(
                admin_application_setup_completion = AdminApplicationSetupCompletion(document_address = adminIdentifier.encodeBytes().toByteString())
            )
        )

        val encodedEvent = event.encode()
        val custom = CustomBuilder().payload(encodedEvent).finish()
        val status = account?.messageSend(groupAddress!!, custom)
        println("send AdminApplicationSetupCompletion status:${status?.name()}")
    }

    suspend fun controllerPairwiseRequest() {
        organisationIdentifier = account?.keychainSigningCreate()
        val requestId = Random.nextBytes(20).toByteString()
        val event = Event(
            header_ = Header(version = Version.V1),
            content = Content(
                controller_pairwise_identity_negotiation_request = ControllerPairwiseIdentityNegotiationRequest(
                    header_ = RequestHeader(request_id = requestId),
                    identity_address = organisationIdentifier!!.encodeBytes().toByteString()
                )
            )
        )
        val encodedEvent = event.encode()
        val custom = CustomBuilder().payload(encodedEvent).finish()
        val status = account?.messageSend(groupAddress!!, custom)
        println("send ControllerPairwiseIdentityNegotiationRequest status:${status?.name()} - request_id:${requestId}")
    }
    fun receivedControllerPairwiseResponse(response: ControllerPairwiseIdentityNegotiationResponse) {
        println("received ControllerPairwiseIdentityNegotiationResponse responseTo:${response.header_?.response_to} ${response.controller_address} - ${response.invocation_address}")
        controllerIdentifier = PublicKey.decodeBytes(response.controller_address.toByteArray())
        controllerInvocation = PublicKey.decodeBytes(response.invocation_address.toByteArray())
    }

    suspend fun confirmOrganisationDetailsRequest() {
        if (organisationIdentifier == null || controllerIdentifier == null || controllerInvocation == null) {
            return
        }
        val organisationCredential = com.joinself.selfsdk.credential.CredentialBuilder()
            .credentialType("OrganisationCreationDetailsCredential")
            .credentialSubject(Address.aure(organisationIdentifier!!))
            .credentialSubjectClaims(hashMapOf("organisationCreationDetails" to hashMapOf(
                "organisationName" to "Self")))
            .issuer(Address.key(inboxAddress!!))
            .signWith(inboxAddress!!, Timestamp.now())
            .validFrom(Timestamp.now())
            .finish()
        val organisationVerifiableCredential = account!!.credentialIssue(organisationCredential!!)
        val organisationPresentation = PresentationBuilder()
            .presentationType("OrganisationCreationDetailsPresentation")
            .holder(Address.key(organisationIdentifier!!))
            .credentialAdd(organisationVerifiableCredential)
            .finish()
        val organisationVerifiablePresentation = account!!.presentationIssue(organisationPresentation)
        val encodedPresentation = organisationVerifiablePresentation!!.encodeBytes()

        val organisationOperation = OperationBuilder()
            .identifier(organisationIdentifier!!)
            .sequence(0)
            .timestamp(Timestamp.now())
            .grantReferenced(Method.AURE, controllerIdentifier!!, controllerInvocation!!, RoleSet(Role.INVOCATION))
            .signWith(organisationIdentifier!!)
            .signWith(controllerInvocation!!)
            .finish()
        account?.identitySign(organisationOperation!!)
        val encodedOperation = organisationOperation!!.encodeBytes()

        val presentations = mutableListOf<ByteString>()
        presentations.add(encodedPresentation.toByteString())
        val adminPresentation = account?.presentationLookupByPresentationType("AdminApplicationPresentation")?.firstOrNull()
        if (adminPresentation != null) {
            println("adminPresentation found")
            presentations.add(adminPresentation.encodeBytes().toByteString())
        } else if (adminApplicationPresentation != null) {
            presentations.add(adminApplicationPresentation!!.encodeBytes().toByteString())
        }

        val requestId = Random.nextBytes(20).toByteString()
        val event = Event(
            header_ = Header(version = Version.V1),
            content = Content(
                identity_details_confirmation_request = IdentityDetailsConfirmationRequest(
                    header_ = RequestHeader(request_id = requestId),
                    identity_address = organisationIdentifier!!.encodeBytes().toByteString(),
                    identity_operation = encodedOperation.toByteString(),
                    presentations = presentations
                )
            )
        )
        val encodedEvent = event.encode()
        val custom = CustomBuilder().payload(encodedEvent).finish()
        val status = account?.messageSend(groupAddress!!, custom)
        println("send IdentityDetailsConfirmationRequest status:${status?.name()} - request_id:${requestId}")
    }

    var adminOrganisationPresentation: VerifiablePresentation? = null
    var adminOrganisationOperation: Operation? = null
    suspend fun confirmOrganisationDetailsResponse(response: IdentityDetailsConfirmationResponse) {
        println("received IdentityDetailsConfirmationResponse")
        val organisationIdentifier = PublicKey.decodeBytes(response.identity_address.toByteArray())

        adminOrganisationPresentation = VerifiablePresentation.decodeBytes(response.presentations.first().toByteArray())
        adminOrganisationOperation = Operation.decodeBytes(organisationIdentifier, response.identity_operation.toByteArray())
    }

    suspend fun confirmControllerApprovalRequest() {
        if (organisationIdentifier == null || controllerIdentifier == null || controllerInvocation == null) {
            println("controllerIdentifier is null")
            return
        }
        if (adminOrganisationPresentation == null || adminOrganisationOperation == null) {
            println("operation is null")
            return
        }

        val encodedOperation = adminOrganisationOperation!!.encodeBytes()
        val encodedPresentation = adminOrganisationPresentation!!.encodeBytes()
        val requestId = Random.nextBytes(20).toByteString()
        val event = Event(
            header_ = Header(version = Version.V1),
            content = Content(
                controller_identity_creation_approval_request = ControllerIdentityCreationApprovalRequest(
                    header_ = RequestHeader(request_id = requestId),
                    identity_address = organisationIdentifier!!.encodeBytes().toByteString(),
                    identity_operation = encodedOperation.toByteString(),
                    presentations = listOf(encodedPresentation.toByteString()),
                    controller_address = controllerIdentifier!!.encodeBytes().toByteString(),
                    invocation_address = controllerInvocation!!.encodeBytes().toByteString()
                )
            )
        )
        val encodedEvent = event.encode()
        val custom = CustomBuilder().payload(encodedEvent).finish()
        val status = account?.messageSend(groupAddress!!, custom)
        println("send ControllerIdentityCreationApprovalRequest status:${status?.name()} - request_id:${requestId}")
    }

    suspend fun confirmControllerApprovalResponse(response: ControllerIdentityCreationApprovalResponse) {
        println("received ControllerIdentityCreationApprovalResponse")
        val organisationIdentifier = PublicKey.decodeBytes(response.identity_address.toByteArray())

        val organisationPresentation = VerifiablePresentation.decodeBytes(response.presentations.first().toByteArray())
        val controllerOperation = Operation.decodeBytes(organisationIdentifier, response.identity_operation.toByteArray())

        suspendCancellableCoroutine { continuation ->
            account?.identityExecute(controllerOperation) {status ->
                println("identityExecute controllerOperation status:${status.name()} ${status.errorMessage()}")
                continuation.resumeWith(Result.success(status))
            }
        }

//        verifyOrganisation(organisationPresentation)
        controllerKYCRequest()
    }

    suspend fun verifyOrganisation(presentation: VerifiablePresentation) {
        val verificationRequest = CredentialVerificationRequestBuilder()
            .credentialType(CredentialType.ORGANISATION)
            .parameters("subject", Address.aure(organisationIdentifier!!).toString())
            .parameters("authorizedPerson", Address.aure(controllerIdentifier!!).toString())
            .proof(presentation)
            .expires(Timestamp.now() + 3600)
            .finish()

        val issuerDocument = suspendCancellableCoroutine { continuation ->
            account?.identityResolve(address = issuerAddress) { status: SelfStatus, document: Document? ->
                continuation.resumeWith(Result.success(document))
            }
        }
        if (issuerDocument == null) {
            println("issuerDocument is null")
            return
        }
        val issuerInboxAddress = issuerDocument.signingKeysWithRoles(RoleSet(Role.MESSAGING)).firstOrNull()
        if (issuerInboxAddress == null) {
            println("inboxAddress is null")
            return
        }

        val group = connectWith(issuerInboxAddress)
        if (group == null) {
            println("group is null")
            return
        }

        val status = account?.messageSend(toAddress = group, verificationRequest)
        println("send verifyOrganisation credential request status:${status?.name()}")
    }

    suspend fun controllerKYCRequest() {
        val livenessPredicate = Predicate.contains(CredentialField.TYPE, CredentialType.LIVENESS_AND_FACIAL_COMPARISON)
            .and(Predicate.notEmpty(CredentialField.SUBJECT_LIVENESS_AND_FACIAL_COMPARISON_SOURCE_IMAGE_HASH))

        val passportPredicate = Predicate.contains(CredentialField.TYPE, CredentialType.PASSPORT)
            .and(Predicate.notEmpty(CredentialField.SUBJECT_PASSPORT_GIVEN_NAMES))
        val predicatesTree = PredicateTree.create(passportPredicate)

        val credentialRequest = CredentialPresentationRequestBuilder()
            .presentationType("OrganisationPresentation")
            .predicates(predicatesTree)
            .expires(Timestamp.now() + 3600)
            .finish()

        val sendStatus = account?.messageSend(groupAddress!!, credentialRequest)
        println("send controller KYC presentation request status: ${sendStatus?.name()}")
    }

    private suspend fun connectWith(other: PublicKey): PublicKey? {
        val deferred = CompletableDeferred<PublicKey>()
        return withTimeoutOrNull(20_000) {
            try {
                onReceivedWelcomeCallback = { welcome ->
                    account?.connectionAccept(asAddress = welcome.toAddress(), welcome = welcome.welcome()) { status: SelfStatus, groupAddress: PublicKey ->
                        println("connectionAccept status:${status.name()} - toAddress:${welcome.fromAddress().encodeHex()}")
                        deferred.complete(groupAddress)
                    }
                }

                account?.connectionNegotiate(asAddress = inboxAddress!!, withAddress = other, expires = Timestamp.now() + 360)
                deferred.await()
            } finally {
            }
        }
    }

    private suspend fun inboxOpen(account: Account): PublicKey? {
        return suspendCancellableCoroutine { continuation ->
            account.inboxOpen(expires = 0L) { status: SelfStatus, address: PublicKey ->
                println("inbox open status:${status.name()} - address:${address.encodeHex()}")
                if (status.success()) {
                    continuation.resumeWith(Result.success(address))
                } else {
                    continuation.resumeWith(Result.success(null))
                }
            }
        }
    }

    private suspend fun generateQrCode(account: Account) {
        inboxAddress = inboxOpen(account)

        if (inboxAddress == null) {
            throw Exception("Can't open inbox")
        }
        println("\n")
        println("server address: ${inboxAddress!!.encodeHex()}")

        val expires = Timestamp.now() + 3600
    val keyPackage = account.connectionNegotiateOutOfBand(inboxAddress!!, expires)
        val discoveryRequest = DiscoveryRequestBuilder()
        .keyPackage(keyPackage)
            .expires(expires)
            .finish()
        val anonymousMessage = AnonymousMessage.fromContent(discoveryRequest)
//        anonymousMessage.setFlags(FlagSet(Flag.TARGET_SANDBOX))
        val qrCodeBytes = anonymousMessage.encodeQR(QrEncoding.UNICODE)
        val qrCodeString = qrCodeBytes.decodeToString()
        println(qrCodeString)
        println()
    }

    suspend fun printQRCode(content: String) {
        val width = 30
        val height = 30

        try {
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            )

            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            for (y in 0 until matrixHeight) {
                for (x in 0 until matrixWidth) {
                    print(if (bitMatrix.get(x, y)) "██" else "  ") // Use two characters for better aspect ratio
                }
                println()
            }

        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            AdminApp().run()
        }
    }
}
