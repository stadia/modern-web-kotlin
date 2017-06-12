package services

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import java.nio.charset.Charset
import java.util.HashMap

/**
 * Created by jeff.yd on 09/06/2017.
 */
class WeatherService {
    fun getTemperature(lat: Double, lon: Double): Promise<Double?, Exception> = task {
        val url = "http://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lon}&units=metric&appid=d06f9fa75ebe72262aa71dc6c1dcd118"
        val (request, response, result) = url.httpGet().responseString()
        val jsonStr = String(response.data, Charset.forName("UTF-8"))
        val json = Gson().fromJson<HashMap<String, Any>>(jsonStr)["main"] as? LinkedTreeMap<String, Double>
        json?.get("temp")
    }
}