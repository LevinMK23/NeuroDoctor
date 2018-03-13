package com.example.roma6.perci
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.loopj.android.http.*;
import cz.msebera.android.httpclient.Header;
import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.os.AsyncTask
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import com.gigamole.library.PulseView
import com.hitomi.cmlibrary.CircleMenu
import com.hitomi.cmlibrary.OnMenuSelectedListener
import ru.yandex.speechkit.*
import com.loopj.android.http.TextHttpResponseHandler
import cz.msebera.android.httpclient.client.methods.RequestBuilder.put
import com.loopj.android.http.RequestParams
import com.loopj.android.http.AsyncHttpClient
import org.json.JSONArray
import org.json.JSONObject
import cz.msebera.android.httpclient.client.methods.RequestBuilder.post
import cz.msebera.android.httpclient.protocol.HTTP
import cz.msebera.android.httpclient.message.BasicHeader
import cz.msebera.android.httpclient.entity.StringEntity
import java.lang.ref.SoftReference


class MainActivity : AppCompatActivity(), RecognizerListener, VocalizerListener{

    var diagnosis: String = ""
    var mentions_list: ArrayList<mention>? = null
    data class mention(val id: String, val orth:String, val choice_id: String, val name:String, val common_name:String, val type:String)
    var nlp_response = ""
    val QUESTIONS = arrayListOf("Вы мужчина или женщина?",
                            "Сколько вам лет?",
                            "Вы страдаете ожирением?",
                            "Вы курите?",
                            "Высокий уровень холестерина?",
                            "Гипертензия?",
                            "Диабет?",
                            "Что вас беспокоит?")
    val ANSWERS = arrayListOf<String>("yes", "19", "no", "no", "no", "no", "no")
    var current_question = 7
    var recognized = false
    var first_symptoms: String? = null
    var diabet = false
    var hyper = false
    var cholesterine = false
    var isSmoking = false
    var obesity:Boolean = false
    var age: Int = 0
    var recognized_text: String? = null
    val sex_array = arrayOf("Man", "Woman")
    val ANSWERS_ENG = arrayOf("Yes", "No", "I don't know")
    val ANSWERS_RUS = arrayOf("Да", "Нет", "Не знаю")
    var sex:String? = null
    var is_pulsing:Boolean = false
    var circle_menu:CircleMenu? = null
    var pulseView: PulseView? = null
    val API_KEY_SPEECHKIT = "b73a1425-c99c-419d-bf9f-a4f0a611842a"
    val REQUEST_PERMISSION_CODE = 1
    var recognizer: Recognizer? = null
    var vocalizer: Vocalizer? = null
    var LstText: TextView? = null
    var statusText: TextView? = null
    val YANDEX_TRANSLATE_LINK = "https://translate.yandex.net/api/v1.5/tr.json/translate"
    val YANDEX_TRANSLATE_API_KEY = "trnsl.1.1.20180203T130851Z.e2c11bdcf730c407.db573356d09a90e7df025b5590ac99c4a5bc0aa5"
    var translated_text:String = ""
    val INFERMEDICA_APP_KEY  = "fc0890164fe7c91b7da323e742104ab1"
    val INFERMEDICA_APP_ID = "584ec592"
    var translated:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SpeechKit.getInstance().configure(this, API_KEY_SPEECHKIT)
        OnCreateActions()
        //requestToYandexTranslateApi("hello", "en-ru", 1)
    }


    fun extractNumber(str: String?): String {

        if (str == null || str.isEmpty()) return ""

        val sb = StringBuilder()
        var found = false
        for (c in str.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c)
                found = true
            } else if (found) {
                // If we already found a digit before and this char is not a digit, stop looping
                break
            }
        }
        return sb.toString()
    }
    fun vocalizerSpeak(text : String){
        resetVocalizer()
        vocalizer = Vocalizer.createVocalizer(
                Vocalizer.Language.RUSSIAN,
                text,
                true,
                Vocalizer.Voice.ZAHAR
        )
        vocalizer?.setListener(this@MainActivity)
        vocalizer?.start()
    }

    fun OnCreateActions(){
        mentions_list = ArrayList()
        pulseView = findViewById(R.id.heartpulseview)
        val pulseviewAnim: Animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.animation)
        pulseviewAnim?.repeatCount = Animation.INFINITE
        pulseView?.startAnimation(pulseviewAnim)
        statusText = findViewById(R.id.StatusText)
        LstText = findViewById(R.id.ListenText)
        circle_menu = findViewById(R.id.circle_menu)
        circle_menu?.setMainMenu(Color.parseColor("#FFFFFF"), R.drawable.circulargraphic, R.drawable.palette)
                ?.addSubMenu(Color.parseColor("#FFFFFF"), R.drawable.check)
                ?.addSubMenu(Color.parseColor("#FFFFFF"), R.drawable.error)
                ?.addSubMenu(Color.parseColor("#FFFFFF"), R.drawable.minuscircle)
                ?.setOnMenuSelectedListener (
                    object: OnMenuSelectedListener{
                        override fun onMenuSelected(index:Int){
                           // requestToInfermedicaApiDiagnosis()
                        }
                    }
                )
        pulseView?.setOnClickListener(
        object: View.OnClickListener{
            override fun onClick(p0: View?) {
                if (is_pulsing && recognized && recognized_text != "" && recognized_text != null){
                    is_pulsing = false
                    if (current_question!=1 && current_question!=0 && current_question <7){
                        if (recognized_text?.contains("да", true)!!){
                            ANSWERS.add(current_question, "yes")
                        }
                        else{
                            ANSWERS.add(current_question, "no")
                        }
                    }
                    else if (current_question==0){
                        when {
                            recognized_text?.contains("мужчина", true)!! -> ANSWERS.add(current_question, "yes")
                            recognized_text?.contains("женщина", true)!! -> ANSWERS.add(current_question, "no")
                            else -> current_question--
                        }
                        }

                    else if (current_question == 7 && translated === false){
                        requestToYandexTranslateApi(recognized_text!!, "ru-en")
                        current_question--
                    }
                    else if (current_question == 7 && translated === true){
                        ANSWERS.add(current_question, translated_text)
                        requestToInfermdedicaApiNLP("headache high fever") //translated_text
                        //requestToInfermedicaApiDiagnosis()
                        translated = false
                    }
                    else if (current_question == 1){
                        if (extractNumber(recognized_text) == "")
                            current_question--
                        else{
                            ANSWERS.add(current_question, extractNumber(recognized_text))
                        }

                    }
                    else if (current_question>7){
                        if (QUESTIONS[current_question] !=null){
                            if (recognized_text?.contains("да", true)!!){
                                    ANSWERS.add(current_question, "yes")
                                }
                                else{
                                    ANSWERS.add(current_question, "no")
                                }
                            requestToInfermedicaApiDiagnosis()
                        }
                        else{
                            Toast.makeText(this@MainActivity, "Your diagnosis is " + diagnosis, Toast.LENGTH_LONG).show()
                        }

                    }

                    //requestToYandexTranslateApi(LstText?.text?.toString() ?: "Текст отсутсвует", "ru-en")
                    pulseView?.finishPulse()
                    current_question++
                    resetRecognizer()
                }
                else{
                    is_pulsing = true
                    vocalizerSpeak(QUESTIONS[current_question])
                    createAndStartRecognizer()
                }
            }
        }
        )
        //requestToInfermdedicaApiNLP("i have headache and im coughing today")
    }


    private fun requestToInfermedicaApiDiagnosis(){
        val httpclient = AsyncHttpClient()
        httpclient.addHeader("App-Id", INFERMEDICA_APP_ID)
        httpclient.addHeader("App-Key", INFERMEDICA_APP_KEY)
        httpclient.addHeader("Content-Type", "application/json")
        var sex = if (ANSWERS[0]=="yes") "male" else "female"
        var age1:Int = ANSWERS[1].toInt()
        val jsonparams = JSONObject()
        jsonparams.put("sex", sex)
        jsonparams.put("age", age1)
        val array = JSONArray()
        for (i in 0..(mentions_list!!.size - 1)){
            var obj  = JSONObject()
            obj.put("id", mentions_list!![i]?.id)
            obj.put("choice_id", mentions_list?.get(i)?.choice_id)
            if (mentions_list!![i]?.choice_id == "present" ){
                obj.put("initial", true )}
            array?.put(obj)
        }
        var extras_inside = JSONObject()
        extras_inside.put("disable_groups", true)
        jsonparams.put("evidence", array)
        jsonparams.put("extras", extras_inside)
        val jsonentity = StringEntity(jsonparams.toString())
        httpclient.post(this@MainActivity, "https://api.infermedica.com/v2/diagnosis", jsonentity, "application/json", object:JsonHttpResponseHandler(){

            override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONArray?) {
                val first:JSONObject = response?.get(0) as JSONObject
                val nlp_response_text = first?.getString("mentions")
                nlp_response = nlp_response_text
                Toast.makeText(this@MainActivity, nlp_response, Toast.LENGTH_LONG).show()
            }

            override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {

                var diagnosisJsonArray = response?.get("conditions") as JSONArray?
                diagnosis = diagnosisJsonArray?.getJSONObject(0)?.getString("common_name")!!
                var question: JSONObject? = response?.get("question") as JSONObject?
                var text_rs = question?.getString("text")?: ""
                if (text_rs === ""){
                    requestToYandexTranslateApi(text_rs, "en-ru", 1)}
                else{
                    requestToYandexTranslateApi(diagnosis, "en-ru")}
//                var str:String = ""
//                for (i in 0..(mentions.length() - 1)) {
//                    val item = mentions.getJSONObject(i)
//                    var ment = mention(item?.getString("id")!!, item?.getString("orth")!!, item?.getString("choice_id")!!,
//                            item?.getString("name")!! ,item?.getString("common_name")!!, item?.getString("type")!!)
//                    mentions_list?.plusElement(ment)
//                    str+=ment.common_name+ ' '
//                }
//                Toast.makeText(this@MainActivity, str, Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<out Header>?, responseString: String?, throwable: Throwable?) {
                Toast.makeText(this@MainActivity, "Oops! Something went wrong!", Toast.LENGTH_LONG).show()
            }
        }
        )
    }

    private fun requestToInfermdedicaApiNLP(text:String){
        val httpclient = AsyncHttpClient()
        httpclient.addHeader("App-Id", INFERMEDICA_APP_ID)
        httpclient.addHeader("App-Key", INFERMEDICA_APP_KEY)
        httpclient.addHeader("Content-Type", "application/json")


        val jsonparams = JSONObject()
        jsonparams.put("text", text)
        val jsonentity = StringEntity(jsonparams.toString())


        httpclient.post(this@MainActivity, "https://api.infermedica.com/v2/parse", jsonentity, "application/json", object:JsonHttpResponseHandler(){

//            override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONArray?) {
//                val first:JSONObject = response?.get(0) as JSONObject
//                val nlp_response_text = first?.getString("mentions")
//                nlp_response = nlp_response_text
//                Toast.makeText(this@MainActivity, nlp_response, Toast.LENGTH_LONG).show()
//            }

            override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
                var mentions: JSONArray = response?.getJSONArray("mentions")!!
                var str:String = ""
                Toast.makeText(this@MainActivity, mentions?.toString(), Toast.LENGTH_LONG).show()
                for (i in 0..(mentions.length() - 1)) {
                    val item = mentions.getJSONObject(i)
                    var ment = mention(item?.getString("id")!!, item?.getString("orth")!!, item?.getString("choice_id")!!,
                            item?.getString("name")!! ,item?.getString("common_name")!!, item?.getString("type")!!)
                    mentions_list?.add(ment)
                    str+=ment.common_name+ ' '
                }
                requestToInfermedicaApiDiagnosis()
                Toast.makeText(this@MainActivity, str, Toast.LENGTH_LONG).show()
            }

            override fun onFailure(statusCode: Int, headers: Array<out Header>?, responseString: String?, throwable: Throwable?) {
                Toast.makeText(this@MainActivity, "Oops! Something went wrong!", Toast.LENGTH_LONG).show()
            }


        } )
        //Toast.makeText(this@MainActivity, nlp_response, Toast.LENGTH_LONG).show()

    }

    private fun requestToYandexTranslateApi(text:String, lang:String, A: Int = 0){
        val params = RequestParams()
        var firstEvent:JSONObject? = null
        var tweetText = ""
        params.add("key", YANDEX_TRANSLATE_API_KEY)
        params.add("text", text)
        params.add("lang", lang)
        params.add("format", "plain")
        YandexTranslateRest.post("", params, object:JsonHttpResponseHandler(){
            override fun onSuccess(statusCode:Int, headers:Array<Header>, response:JSONObject)
            {
                var textjsonobj = response.getJSONArray("text")
                translated_text = textjsonobj[0] as String
                if (A===1){
                    QUESTIONS.add(translated_text!!)
                    current_question++
                }
                translated = true
            }
            override fun onSuccess(statusCode:Int, headers:Array<Header>, timeline:JSONArray) {
                firstEvent = timeline.get(0) as JSONObject
                tweetText = firstEvent?.getString("text")!!
                Toast.makeText(this@MainActivity, firstEvent.toString()+"Array", Toast.LENGTH_LONG).show()
                if (A===1){
                    QUESTIONS.add(tweetText)
                    current_question++
                }
                translated_text = tweetText
                Toast.makeText(this@MainActivity, tweetText, Toast.LENGTH_LONG).show()
                translated = true
            }
            override fun onFailure(statusCode: Int, headers: Array<out Header>?, responseString: String?, throwable: Throwable?) {
                Toast.makeText(this@MainActivity, "Oops..Something went wrong", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun resetVocalizer() {
        vocalizer?.cancel()
        vocalizer = null
    }

    override fun onPlayingBegin(p0: Vocalizer?) {
    }

    override fun onVocalizerError(p0: Vocalizer?, p1: Error?) {
    }

    override fun onSynthesisDone(p0: Vocalizer?, p1: Synthesis?) {
    }

    override fun onPlayingDone(p0: Vocalizer?) {
        createAndStartRecognizer()
    }

    override fun onSynthesisBegin(p0: Vocalizer?) {
    }
    override fun onRecordingDone(recognizer: Recognizer) {
        updateStatus("Recording done")
    }

    override fun onRecordingBegin(recognizer: Recognizer) {
        updateStatus("Recording begin")
    }

    override fun onSpeechDetected(recognizer: Recognizer) {
        updateStatus("Speech detected")
    }

    override fun onSpeechEnds(recognizer: Recognizer) {
        updateStatus("Speech ends")
    }

    fun resetRecognizer(){
        recognizer?.cancel()
        //pulseView?.finishPulse()
        recognizer = null
        recognized = false

    }

    fun createAndStartRecognizer(){
        val context: MainActivity = this ?: return
        if (ContextCompat.checkSelfPermission(context, RECORD_AUDIO) != PERMISSION_GRANTED) {
            requestPermissions(arrayOf(RECORD_AUDIO), REQUEST_PERMISSION_CODE);
        } else {
            resetRecognizer();
            recognizer = Recognizer.create(
                    Recognizer.Language.RUSSIAN,
                    Recognizer.Model.NOTES,
                    this
            )
            pulseView?.startPulse()
            recognizer?.start()
        }
    }
    
    override fun onSoundDataRecorded(recognizer: Recognizer, bytes: ByteArray) {}

    override fun onPowerUpdated(recognizer: Recognizer, power: Float) {
        updateProgress(100)
    }

    override fun onPartialResults(recognizer: Recognizer, recognition: Recognition, b: Boolean) {
        updateStatus("Partial results " + recognition.bestResultText)
    }

    override fun onRecognitionDone(recognizer: Recognizer, recognition: Recognition) {
        updateResult(recognition.bestResultText)
        recognized = true
    }

    private fun updateResult(text: String) {
        recognized_text = text
        LstText?.text = recognized_text!!
    }

    private fun updateStatus(text: String) {
        statusText?.text = text
    }

    private fun updateProgress(progress: Int) {

    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode != REQUEST_PERMISSION_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.size == 1 && grantResults[0] == PERMISSION_GRANTED) {
            createAndStartRecognizer()
        } else {
            updateStatus("Record audio permission was not granted")
        }
    }
    override fun onError(recognizer: Recognizer, error: Error) {
        if (error.code == Error.ERROR_CANCELED) {
            updateStatus("Cancelled")
            updateProgress(0)
        } else {
            updateStatus("Error occurred " + error.string)
            resetRecognizer()
        }
    }
}
