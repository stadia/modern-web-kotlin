package model

import io.vertx.core.AsyncResult
import io.vertx.core.CompositeFuture
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AbstractUser
import io.vertx.ext.auth.AuthProvider
import kotliquery.Row
import java.util.*

/**
 * Created by jeff.yd on 12/06/2017.
 */
class DatabaseUser(val id: UUID, val username: String, val passwordHash: String): AbstractUser() {
    override fun principal(): JsonObject {
        return JsonObject().put("username", username)
    }

    override fun setAuthProvider(authProvider: AuthProvider?) {
    }

    override fun doIsPermitted(permission: String?, resultHandler: Handler<AsyncResult<Boolean>>?) {
        val result = CompositeFuture.factory.succeededFuture(true)
        resultHandler?.handle(result)
    }

    companion object {
        fun fromDb(row: Row): DatabaseUser {
            return DatabaseUser(UUID.fromString(row.string("user_id")), row.string("user_code"), row.string("password"))
        }
    }
}