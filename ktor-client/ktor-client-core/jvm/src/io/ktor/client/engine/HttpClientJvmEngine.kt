package io.ktor.client.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.scheduling.*
import kotlin.coroutines.*

/**
 * Base jvm implementation for [HttpClientEngine]
 */
@Suppress("KDocMissingDocumentation")
abstract class HttpClientJvmEngine(engineName: String) : HttpClientEngine {
    private val clientContext = CompletableDeferred<Unit>()
    private val callSupervisor = SupervisorJob(clientContext)

    @UseExperimental(InternalCoroutinesApi::class)
    override val dispatcher: ExperimentalCoroutineDispatcher by lazy {
        ExperimentalCoroutineDispatcher(config.threadsCount)
    }

    @UseExperimental(InternalCoroutinesApi::class)
    override val coroutineContext: CoroutineContext by lazy {
        dispatcher + clientContext + CoroutineName("$engineName-context")
    }

    /**
     * Create [CoroutineContext] to execute call.
     */
    protected fun createCallContext(): CoroutineContext = coroutineContext + CompletableDeferred<Unit>(callSupervisor)

    override fun close() {
        callSupervisor.cancel()

        callSupervisor.invokeOnCompletion {
            clientContext.complete(Unit)
        }

        clientContext.invokeOnCompletion {
            GlobalScope.launch(dispatcher) {
                @UseExperimental(InternalCoroutinesApi::class)
                dispatcher.close()
            }
        }
    }
}
