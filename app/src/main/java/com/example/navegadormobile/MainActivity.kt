package com.example.navegadormobile

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_main.view. *
import kotlinx.android.synthetic.main.settings_dialog.view.*

class MainActivity : AppCompatActivity() {

    var mainLayout: View? = null
    var toolbar: Toolbar? = null
    var webView: WebView? = null

    //Cria um arquivo dentro do cel do usuário com suas configs/preferencias
    val SHAREDPREF_FILENAME = "com.example.navegadormobile.web_prefs"
    var sharedPref: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainLayout = layoutInflater.inflate(R.layout.activity_main, null)
        setContentView(mainLayout)

        title = ""

        //Pegando as preferencias do usuario (de config)
        sharedPref = this.getSharedPreferences(SHAREDPREF_FILENAME,0)

        toolbar = this.mainLayout?.toolbar
        setSupportActionBar(toolbar)

        var LoadLastViewed : Boolean = false
        LoadLastViewed = sharedPref!!.getBoolean("LoadLastURL", false)

        //Verifica se o usario salvou um pagina como inicial, caso contrario exibira do google por padrao
        if(LoadLastViewed) {
            sharedPref!!.getString("LastVisitedURL", "https://google.com")?.let { setUrl(it) }
        } else {
            sharedPref!!.getString("HomePageURL", "https://google.com")?.let { setUrl(it) }
        }
    }

    @SuppressLint("SetJavaScriptEnable") //Indica que o Lint deve ignorar os avisos especificados
    //para o elemento anotado. Usado para poder usar o Javascript dentro do navegador
    private fun setUrl(url: String) {
        var webUrl = url

        //Caso o usario nao digite o http antes do site, irá add este prefixo automaticante
        if(!webUrl.startsWith("http")) {
            webUrl = "http://$url"
        }

        //Ativando o javascript do navegador
        webView = findViewById<WebView>(R.id.webBrowser)
        webView!!.getSettings().javaScriptEnabled = true
        webView!!.getSettings().javaScriptCanOpenWindowsAutomatically = true
        webView!!.webViewClient = object  : WebViewClient() {}

        //Carregando a url
        webView!!.loadUrl(webUrl)
        mainLayout?.webUrl!!.setText(webUrl)

        sharedPref?.edit()?.putString("LastVisitedURL", webUrl)?.apply()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    //Função ativada quando um item for clicado
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.getItemId()

        when (id) {
            R.id.actionGo -> navigation()
            R.id.actionHome -> home()
            R.id.actionRefresh -> reload()
            R.id.actionSettings -> config()
            else -> showToast(getString(R.string.no_click))
        }

        return super.onOptionsItemSelected(item)
    }

    //Função ativada quando ocorrer algum erro
    private fun showToast(message: String) {
        Toast.makeText(this.applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    //Exibe uma janela de configurações do navegador
    private fun config() {
        val alertDialogBuilder = AlertDialog.Builder(this)

        //Chama o layout do dialog
        val dialogLayout = layoutInflater.inflate(R.layout.settings_dialog, null)
        alertDialogBuilder.setView(dialogLayout)

        //Chamando os itens da config
        val homePageEditText = dialogLayout.txtHomePageUrl as EditText
        homePageEditText.setText(sharedPref!!.getString("HomePageURL", "https://google.com"))

        val checkedTextView = dialogLayout.checkedTextView as CheckedTextView
        var showCheck = sharedPref!!.getBoolean("LoadLastURL", false)
        checkedTextView.setCheckMarkDrawable(
            //Se estiver selecionado, mostrará o icone checked
            if(showCheck) R.drawable.checked
            else R.drawable.unchecked
        )

        checkedTextView.setOnClickListener { //Interruptor true/false
            checkedTextView.isChecked = !checkedTextView.isChecked()
            checkedTextView.setCheckMarkDrawable(
                //Se estiver selecionado, mostrará o icone checked
                if(checkedTextView.isChecked()) R.drawable.checked
                else R.drawable.unchecked)
            sharedPref?.edit()?.putBoolean(("LoadLastURL"), checkedTextView.isChecked())?.apply()
            }

        //Criando botão de salvar e cancelar
        alertDialogBuilder
            .setCancelable(false)
            .setPositiveButton(getString(R.string.btn_save)
            ) {dialog, id ->
                sharedPref?.edit()?.putString("HomePageURL", homePageEditText.text.toString())?.apply()
            }
            .setNegativeButton(getString(R.string.btb_cancel)
            ){dialog, id -> dialog.cancel()}

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun reload() {
        val webUrlString = webView!!.url

        if(webUrlString.isNullOrEmpty()) { //verificando se há conteudo
            //Caso tenha algo em mainlayout,irá fazer o reload
            if(!mainLayout?.webUrl!!.getText().trim().isNullOrEmpty()) {
                setUrl(mainLayout?.webUrl!!.text.toString())
            }
        } else {
            setUrl(webUrlString)
        }
    }

    private fun home() {
        sharedPref!!.getString("HomePageURL", "https://google.com")?.let { setUrl(it) }
    }

    //Se houver algum conteudo na barra de navegação, ele irá fazer uma pesquisar, se estiver
    //nulo/vazio, mostrará uma msg de erro do showToast
    private fun navigation() {
        if(mainLayout?.webUrl!!.text.trim().isNullOrEmpty()) {
            showToast(getString(R.string.no_url))
        } else {
            setUrl(mainLayout?.webUrl!!.text.toString())
        }
    }

    //Botão padrao de voltar do android, ao inves de sair do app, irá retornar para a pagina anterior
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if(event.getAction() === KeyEvent.ACTION_DOWN) { //verifica se algum botao foi apertado
            when(keyCode) { //busca qual botão foi apertado
                KeyEvent.KEYCODE_BACK -> { //Verifica se foi apertado o botao de voltar padrao do android
                    if(webView!!.canGoBack()) { //Vê se há alguma página para voltar, caso contrário, não faz nada
                        webView!!.goBack()
                    } else {
                        finish()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}