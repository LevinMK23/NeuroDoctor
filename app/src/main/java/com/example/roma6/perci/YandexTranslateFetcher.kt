import android.content.Context
import com.loopj.android.http.*

object YandexTranslateRest {
    private val BASE_URL = "https://translate.yandex.net/api/v1.5/tr.json/translate"

    private val client = AsyncHttpClient()

    fun get(url: String, params: RequestParams, responseHandler: AsyncHttpResponseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler)
    }

    fun post(url: String, params: RequestParams, responseHandler: JsonHttpResponseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler)
    }

    private fun getAbsoluteUrl(relativeUrl: String): String {
        return BASE_URL + relativeUrl
    }
}


object InfermedicaTranslateRest {
    private val BASE_URL = "https://api.infermedica.com/v2/"

    private val client = AsyncHttpClient()

    fun get(url: String, params: RequestParams, responseHandler: AsyncHttpResponseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler)
    }

    fun post( url: String, params: RequestParams, responseHandler: AsyncHttpResponseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler)
    }

    private fun getAbsoluteUrl(relativeUrl: String): String {
        return BASE_URL + relativeUrl
    }
}