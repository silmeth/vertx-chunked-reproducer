package chunked.reproducer

import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpMethod
import io.vertx.core.Vertx
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.junit.jupiter.api.AfterAll
import org.asynchttpclient.Dsl
import java.util.concurrent.CompletableFuture
import java.time.Duration

class ChunkedTest {
    @Test
    fun chunkedResponse() {
        val testFinished = CompletableFuture<Unit>()

        server
            .compose {
                val client = vertx.createHttpClient()
                client.request(HttpMethod.GET, port, "localhost", "/")
            }
            .map { it.exceptionHandler { e -> testFinished.fail(e) } }
            .compose(HttpClientRequest::connect)
            .compose { res ->
                assertEquals(200, res.statusCode())
                res.body().map { res to it }
            }
            .onSuccess { (res, body) ->
                assertEquals("true", res.getTrailer("finished"))
                assertEquals("chunk 1chunk 2final chunk", body.toString(Charsets.UTF_8))
                testFinished.done()
            }
            .onFailure { testFinished.fail(it) }

        testFinished.join()
    }


    @Test
    fun chunkedResponseHeaderHack(): Unit = assertTimeoutPreemptively(Duration.ofSeconds(3L)) {
        val testFinished = CompletableFuture<Unit>()

        server
            .compose {
                val client = vertx.createHttpClient()
                client.request(HttpMethod.GET, port, "localhost", "/")
            }
            .map { it.putHeader("header", "hack") }
            .map { it.exceptionHandler { e -> testFinished.fail(e) } }
            .compose(HttpClientRequest::connect)
            .compose { res ->
                assertEquals(200, res.statusCode())
                res.body().map { res to it }
            }
            .onSuccess { (res, body) ->
                assertEquals("true", res.getTrailer("finished"))
                assertEquals("chunk 1chunk 2final chunk", body.toString(Charsets.UTF_8))
                testFinished.done()
            }
            .onFailure { testFinished.fail(it) }

        testFinished.join()
    }

    @Test
    fun chunkedResponseAsyncHttpClient() {
        val testFinished = CompletableFuture<Unit>()

        server
            .toCompletionStage()
            .thenCompose {
                val client = Dsl.asyncHttpClient()
                client.prepareGet("http://localhost:$port/").execute().toCompletableFuture()
            }
            .thenApplyAsync { res ->
                assertEquals(200, res.statusCode)
                assertEquals("true", res.getHeader("finished"))
                assertEquals("chunk 1chunk 2final chunk", res.responseBody)
                testFinished.done()
            }
            .exceptionally { testFinished.fail(it) }

        testFinished.join()
    }

    fun CompletableFuture<Unit>.fail(e: Throwable) = this.completeExceptionally(e)
    fun CompletableFuture<Unit>.done() = this.complete(Unit)

    companion object {
        private const val port = 9768
        private val vertx = Vertx.vertx()
        private val server = vertx.createHttpServer()
            .requestHandler {
                val response = it.response().setStatusCode(200).setChunked(true)
                    .exceptionHandler { e ->
                        System.err.println("Error in response handler!")
                        e.printStackTrace()
                    }

                response.write("chunk 1")
                    .compose { response.write("chunk 2") }
                    .compose { response.putTrailer("finished", "true").end("final chunk") }
            }
            .listen(port)

        @AfterAll
        fun closeVertx() {
            vertx.close().toCompletionStage().toCompletableFuture().join()
        }
    }
}