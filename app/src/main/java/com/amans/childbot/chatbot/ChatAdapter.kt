package com.amans.childbot.chatbot

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.widget.TextView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.recyclerview.widget.RecyclerView
import com.amans.childbot.databinding.ChatItemBinding
import com.amans.childbot.pages.WebViewActivity

class ChatAdapter(private val context: Context,private var messages: List<MessageModel>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(val binding: ChatItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ChatItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        with(holder.binding) {
            if (message.role == "user") {
                rightChatView.visibility = View.VISIBLE
                leftChatView.visibility = View.GONE
                rightChatTextView.text = message.message
            } else {
                rightChatView.visibility = View.GONE
                leftChatView.visibility = View.VISIBLE
                formatTextWithLinks(context,holder.binding.leftChatTextView,message.message)
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<MessageModel>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    fun formatTextWithLinks(context: Context, textView: TextView, text: String) {
        val builder = SpannableStringBuilder(text)

        // 🔹 Remove `**` and Apply Bold Formatting
        val boldPattern = Regex("\\*\\*(.*?)\\*\\*")
        boldPattern.findAll(text).toList().asReversed().forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1

            // Extract the bold text without "**"
            val boldText = match.groupValues[1]

            // Replace "**text**" with "text" and apply bold styling
            builder.replace(start, end, boldText)
            builder.setSpan(StyleSpan(Typeface.BOLD), start, start + boldText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // 🔹 Detect URLs and Make Them Clickable
        val matcher = Patterns.WEB_URL.matcher(builder)
        while (matcher.find()) {
            val url = matcher.group()
            if (url != null) {
                val start = matcher.start()
                val end = matcher.end()
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val intent = Intent(context, WebViewActivity::class.java)
                        intent.putExtra("url", url)
                        context.startActivity(intent)  // ✅ Open WebViewActivity
                    }
                }
                builder.setSpan(clickableSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        textView.text = builder
        textView.movementMethod = LinkMovementMethod.getInstance() // 🔹 Enable clicking
    }

}


