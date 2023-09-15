package com.dacosys.warehouseCounter.data.ktor.v1.functions

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorDacoService
import com.dacosys.warehouseCounter.data.ktor.v1.service.PackagesResult
import com.dacosys.warehouseCounter.misc.Statics.Companion.APP_VERSION_ID
import com.dacosys.warehouseCounter.misc.Statics.Companion.APP_VERSION_ID_IMAGECONTROL
import com.dacosys.warehouseCounter.misc.objects.status.ProgressStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

class GetClientPackages private constructor(builder: Builder) {

    private var _result: ArrayList<com.dacosys.warehouseCounter.data.ktor.v1.dto.clientPackage.Package> = ArrayList()

    private var _onEvent: (PackagesResult) -> Unit = {}
    private var _email = ""
    private var _password = ""
    private var _installationCode = ""

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        ktorDacoService.getClientPackage(body, callback = {
            val packageList = ArrayList(it.packages.values)

            for (p in packageList) {
                val installationCode = p.installationCode
                if (_installationCode.isNotEmpty() && installationCode != _installationCode) {
                    continue
                }

                val productId = p.productVersionId
                if (p.active == 1 && (productId == APP_VERSION_ID || productId == APP_VERSION_ID_IMAGECONTROL)) {
                    _result.add(p)
                }
            }

            if (_result.any()) sendEvent(context.getString(R.string.ok), ProgressStatus.finished)
            else sendEvent(context.getString(R.string.client_has_no_software_packages), ProgressStatus.canceled)
        })
    }

    private val body by lazy {
        // Autentificación del Cliente
        com.dacosys.warehouseCounter.data.ktor.v1.dto.clientPackage.AuthDataCont().apply {
            authData = com.dacosys.warehouseCounter.data.ktor.v1.dto.clientPackage.ClientAuthData().apply {
                version = "1"
                email = _email
                password = _password
            }
        }
    }

    private fun sendEvent(
        msg: String = "",
        status: ProgressStatus = ProgressStatus.unknown
    ) {
        _onEvent.invoke(
            PackagesResult(
                status = status,
                result = _result,
                clientEmail = _email,
                clientPassword = _password,
                msg = msg
            )
        )
    }

    companion object {
        fun getConfig(
            onEvent: (PackagesResult) -> Unit,
            email: String,
            password: String,
            installationCode: String = "",
        ) {
            if (email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                thread {
                    Builder()
                        .onEvent(onEvent)
                        .addParams(
                            email = email,
                            password = password,
                            installationCode = installationCode
                        ).build()
                }
            }
        }
    }

    class Builder {
        fun build(): GetClientPackages {
            return GetClientPackages(this)
        }

        internal var onEvent: (PackagesResult) -> Unit = {}
        internal var email = ""
        internal var password = ""
        internal var installationCode = ""

        fun addParams(
            email: String,
            password: String,
            installationCode: String,
        ): Builder {
            this.email = email
            this.password = password
            this.installationCode = installationCode
            return this
        }

        fun onEvent(onEvent: (PackagesResult) -> Unit): Builder {
            this.onEvent = onEvent
            return this
        }
    }

    init {
        _onEvent = builder.onEvent
        _email = builder.email
        _password = builder.password
        _installationCode = builder.installationCode

        scope.launch {
            coroutineScope {
                withContext(Dispatchers.IO) { suspendFunction() }
            }
        }
    }
}
