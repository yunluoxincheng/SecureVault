package com.securevault.autofill.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.service.autofill.Dataset
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import com.securevault.security.KeyManager
import co.touchlab.kermit.Logger
import org.koin.core.context.GlobalContext
import kotlin.math.max

class AutofillCredentialPickerActivity : Activity() {
    private val keyManager: KeyManager by lazy { GlobalContext.get().get() }
    private val log = Logger.withTag("SvAutofillPicker")
    private val layoutId by lazy { resources.getIdentifier("autofill_dataset_item", "layout", packageName) }
    private val titleId by lazy { resources.getIdentifier("autofill_item_title", "id", packageName) }
    private val subtitleId by lazy { resources.getIdentifier("autofill_item_subtitle", "id", packageName) }
    private val iconId by lazy { resources.getIdentifier("autofill_item_icon", "id", packageName) }
    private val iconRes by lazy { resources.getIdentifier("ic_launcher_foreground", "drawable", packageName) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (keyManager.getDataKey() == null) {
            Toast.makeText(this, "密码库已锁定，请先解锁", Toast.LENGTH_SHORT).show()
            log.w { "picker opened but vault locked, reroute to auth (no NEW_TASK)" }
            startActivity(Intent(this, AutofillAuthActivity::class.java))
            finish()
            return
        }

        val usernameIds = intent.getParcelableArrayListExtra<AutofillId>(EXTRA_USERNAME_IDS).orEmpty()
        val passwordIds = intent.getParcelableArrayListExtra<AutofillId>(EXTRA_PASSWORD_IDS).orEmpty()
        val titles = intent.getStringArrayExtra(EXTRA_TITLES)?.toList().orEmpty()
        val usernames = intent.getStringArrayExtra(EXTRA_USERNAMES)?.toList().orEmpty()
        val passwords = intent.getStringArrayExtra(EXTRA_PASSWORDS)?.toList().orEmpty()

        if (titles.isEmpty() || usernames.size != titles.size || passwords.size != titles.size) {
            log.w { "invalid picker payload, finish. titles=${titles.size} usernames=${usernames.size} passwords=${passwords.size}" }
            finish()
            return
        }
        log.i { "picker ready items=${titles.size} idsU=${usernameIds.size} idsP=${passwordIds.size}" }

        setContentView(
            resources.getIdentifier("autofill_credential_picker", "layout", packageName),
        )
        val listView = findViewById<ListView>(
            resources.getIdentifier("autofill_picker_list", "id", packageName),
        )

        val uniqueItems = linkedMapOf<String, PickerItem>()
        for (i in titles.indices) {
            val title = titles[i]
            val username = usernames[i]
            val password = passwords[i]
            val dedupeKey = "$title|$username|$password"
            if (!uniqueItems.containsKey(dedupeKey)) {
                uniqueItems[dedupeKey] = PickerItem(
                    title = title,
                    subtitle = "账号 ${maskUsername(username)}",
                    username = username,
                    password = password,
                )
            }
        }
        val items = uniqueItems.values.toList()
        log.i { "picker deduped items=${items.size} raw=${titles.size}" }

        listView.adapter = PickerAdapter(items)
        listView.setOnItemClickListener { _, _, position, _ ->
            val picked = items[position]
            val datasetPresentation = createDatasetPresentation(
                title = picked.title,
                subtitle = picked.subtitle,
            )
            val dataset = Dataset.Builder(datasetPresentation).apply {
                usernameIds.forEach { setValue(it, AutofillValue.forText(picked.username)) }
                passwordIds.forEach { setValue(it, AutofillValue.forText(picked.password)) }
            }.build()
            setResult(
                RESULT_OK,
                Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset),
            )
            log.i { "picked index=$position title=${picked.title}" }
            finish()
        }

        applyTopInsetOffset(listView)
        window?.attributes = window?.attributes?.apply { dimAmount = 0.35f }
    }

    private fun maskUsername(username: String): String {
        if (username.length <= 4) return "****"
        return "${username.take(2)}${"*".repeat(username.length - 4)}${username.takeLast(2)}"
    }

    private fun applyTopInsetOffset(listView: ListView) {
        val statusBarHeight = runCatching {
            val id = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (id > 0) resources.getDimensionPixelSize(id) else 0
        }.getOrDefault(0)
        val extraTop = max(statusBarHeight, dp(24))
        listView.setPadding(
            listView.paddingLeft,
            listView.paddingTop + extraTop,
            listView.paddingRight,
            listView.paddingBottom,
        )
        log.i { "picker top offset applied statusBar=$statusBarHeight extra=$extraTop" }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun createDatasetPresentation(title: String, subtitle: String): RemoteViews {
        if (layoutId != 0 && titleId != 0 && subtitleId != 0) {
            return RemoteViews(packageName, layoutId).apply {
                setTextViewText(titleId, title)
                setTextViewText(subtitleId, subtitle)
                if (iconId != 0 && iconRes != 0) {
                    setImageViewResource(iconId, iconRes)
                }
            }
        }
        // Keep compatibility fallback if custom resource lookup fails unexpectedly.
        return RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "$title · $subtitle")
        }
    }

    private data class PickerItem(
        val title: String,
        val subtitle: String,
        val username: String,
        val password: String,
    )

    private inner class PickerAdapter(
        private val items: List<PickerItem>,
    ) : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val row = convertView ?: buildRow(parent)
            val item = items[position]
            val titleView = row.findViewById<TextView>(titleId)
            val subtitleView = row.findViewById<TextView>(subtitleId)
            titleView.text = item.title
            subtitleView.text = item.subtitle
            return row
        }

        private fun buildRow(parent: ViewGroup?): View {
            if (layoutId == 0 || titleId == 0 || subtitleId == 0) {
                log.w { "picker layout resources missing; finish to avoid old single-line fallback" }
                finish()
                return View(this@AutofillCredentialPickerActivity)
            }
            val view = LayoutInflater.from(this@AutofillCredentialPickerActivity)
                .inflate(layoutId, parent, false)
            if (iconId != 0 && iconRes != 0) {
                view.findViewById<View>(iconId)?.let {
                    // keep same icon source as outer autofill item
                    if (it is android.widget.ImageView) it.setImageResource(iconRes)
                }
            }
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            return view
        }
    }

    companion object {
        const val EXTRA_USERNAME_IDS = "picker_username_ids"
        const val EXTRA_PASSWORD_IDS = "picker_password_ids"
        const val EXTRA_TITLES = "picker_titles"
        const val EXTRA_USERNAMES = "picker_usernames"
        const val EXTRA_PASSWORDS = "picker_passwords"
    }
}
