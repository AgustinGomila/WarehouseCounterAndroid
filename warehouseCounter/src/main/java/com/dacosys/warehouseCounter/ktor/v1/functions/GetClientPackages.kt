package com.dacosys.warehouseCounter.ktor.v1.functions

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorDacoService
import com.dacosys.warehouseCounter.ktor.v1.dto.clientPackage.AuthDataCont
import com.dacosys.warehouseCounter.ktor.v1.dto.clientPackage.ClientAuthData
import com.dacosys.warehouseCounter.ktor.v1.dto.clientPackage.Package
import com.dacosys.warehouseCounter.ktor.v1.service.PackagesResult
import com.dacosys.warehouseCounter.misc.Statics.Companion.APP_VERSION_ID
import com.dacosys.warehouseCounter.misc.Statics.Companion.APP_VERSION_ID_IMAGECONTROL
import com.dacosys.warehouseCounter.sync.ProgressStatus
import kotlinx.coroutines.*
import kotlin.concurrent.thread

class GetClientPackages(private val onEvent: (PackagesResult) -> Unit) {

    companion object {
        fun getConfig(
            onEvent: (PackagesResult) -> Unit,
            email: String,
            password: String,
            installationCode: String = "",
        ) {
            if (email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                thread {
                    val get = GetClientPackages(onEvent)
                    get.addParams(email = email, password = password, installationCode = installationCode)
                    get.execute()
                }
            }
        }
    }

    private var myEmail = ""
    private var myPassword = ""
    private var myInstallationCode = ""

    private var r: ArrayList<Package> = ArrayList()

    fun addParams(
        email: String,
        password: String,
        installationCode: String,
    ) {
        this.myEmail = email
        this.myPassword = password
        this.myInstallationCode = installationCode
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        scope.launch {
            coroutineScope {
                withContext(Dispatchers.IO) { suspendFunction() }
            }
        }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        ktorDacoService.getClientPackage(body, callback = {
            val packageList = ArrayList(it.packages.values)

            for (p in packageList) {
                val installationCode = p.installationCode
                if (myInstallationCode.isNotEmpty() && installationCode != myInstallationCode) {
                    continue
                }

                val productId = p.productVersionId
                if (p.active == 1 && (productId == APP_VERSION_ID || productId == APP_VERSION_ID_IMAGECONTROL)) {
                    r.add(p)
                }
            }

            if (r.any()) sendEvent(context.getString(R.string.ok), ProgressStatus.finished)
            else sendEvent(context.getString(R.string.client_has_no_software_packages), ProgressStatus.canceled)
        })
    }

    private val body by lazy {
        // Autentificación del Cliente
        AuthDataCont().apply {
            this.authData = ClientAuthData().apply {
                this.version = "1"
                this.email = myEmail
                this.password = myPassword
            }
        }
    }

    private fun sendEvent(
        msg: String = "",
        status: ProgressStatus = ProgressStatus.unknown
    ) {
        onEvent.invoke(
            PackagesResult(
                status = status,
                result = r,
                clientEmail = myEmail,
                clientPassword = myPassword,
                msg = msg
            )
        )
    }
}