package com.gaplotech

import akka.Done
import akka.actor.ActorSystem
import akka.http.javadsl.ConnectHttp
import akka.http.javadsl.Http
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.Route
import akka.stream.ActorMaterializer
import java.util.concurrent.CompletionStage
import akka.http.javadsl.server.PathMatchers
import com.squareup.moshi.JsonClass
import java.util.concurrent.CompletableFuture

@JsonClass(generateAdapter = true)
data class Item(val foo: String, val itemId: Long)

@JsonClass(generateAdapter = true)
data class Order(
    val id: Int,
    val idOpt: Int?,
    val idDefaultVal: Int = 100,
    val xs: List<Int>,
    val xsDefaultVal: List<Int> = listOf(100)
)

class HttpServerMinimalExampleTest : AllDirectives() {

    // (fake) async database query api
    private fun fetchItem(itemId: Long): CompletionStage<Item?> {
        return CompletableFuture.completedFuture(Item("foo", itemId))
    }

    // (fake) async database query api
    private fun saveOrder(order: Order): CompletionStage<Done> {
        return CompletableFuture.completedFuture(Done.getInstance())
    }

    private fun createRoute(): Route {

        return route(
                get {
                    pathPrefix("item") {
                        path(PathMatchers.longSegment()) { id: Long ->
                            val futureMaybeItem = fetchItem(id)
                            onSuccess(futureMaybeItem) { maybeItem ->
                                maybeItem?.let({ item -> completeOK(item, MoshiMarshaller.marshaller()) })
                                        ?: complete(StatusCodes.NOT_FOUND, "Not Found")
                            }
                        }
                    }
                },
                post {
                    path("create-order") {
                        entity(MoshiMarshaller.unmarshaller(Order::class.java)) { order ->
                            val futureSaved = saveOrder(order)
                            onSuccess(futureSaved) { done ->
                                completeOK(order, MoshiMarshaller.marshaller())
                            }
                        }
                    }
                }
        )
    }
    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            // boot up server using the route as defined below
            val system = ActorSystem.create("routes")

            val http = Http.get(system)
            val materializer = ActorMaterializer.create(system)

            //In order to access all directives we need an instance where the routes are define.
            val app = HttpServerMinimalExampleTest()

            val routeFlow = app.createRoute().flow(system, materializer)
            val binding = http.bindAndHandle(routeFlow,
                    ConnectHttp.toHost("localhost", 9001), materializer)

            println("Server online at http://localhost:9001/\nPress RETURN to stop...")
            System.`in`.read() // let it run until user presses return

            binding
                    .thenCompose<Done> { it.unbind() } // trigger unbinding from the port
                    .thenAccept { unbound -> system.terminate() } // and shutdown when done
        }
    }
}
