package services

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import model.SunInfo
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.nio.charset.Charset
import java.util.HashMap

/**
 * Created by jeff.yd on 09/06/2017.
 */
class SunService {
    fun getSunInfo(lat: Double, lon: Double): Promise<SunInfo, Exception> = task {
        val url = "http://api.sunrise-sunset.org/json?lat=${lat}&lng=${lon}&formatted=0"
        val (request, response, result) = url.httpGet().responseString()
        val jsonStr = String(response.data, Charset.forName("UTF-8"))
        val json = Gson().fromJson<HashMap<String, Any>>(jsonStr)["results"] as? LinkedTreeMap<String, String>
        val sunriseTime = DateTime.parse(json?.get("sunrise"))
        val sunsetTime = DateTime.parse(json?.get("sunset"))
        val formatter = DateTimeFormat.forPattern("HH:mm:ss").
                withZone(DateTimeZone.forID("Asia/Seoul"))
        SunInfo(formatter.print(sunriseTime), formatter.print(sunsetTime))
    }
}