/*
 * Copyright 2021 Rex M. Torres
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.rexmtorres.android.pinentry

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.AnyRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import java.lang.Integer.min

/**
 * A PIN entry widget.
 */
class PinEntryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    companion object {
        //region Accent types
        /**
         * [AccentType] that draws no accent.
         */
        const val ACCENT_NONE = 0
        /**
         * [AccentType] that draws the accents for all characters.
         */
        const val ACCENT_ALL = 1
        /**
         * [AccentType] that draws the accent for only the focused character.
         */
        const val ACCENT_CHARACTER = 2
        //endregion Accent types
    }

    /**
     * Specifies the required length of the PIN.
     */
    val digits: Int

    /**
     * Specifies whether the PIN is a [number][InputType.TYPE_CLASS_NUMBER] or
     * [text][InputType.TYPE_CLASS_TEXT].
     */
    val inputType: Int

    /**
     * Specifies the width of each character field view.
     */
    val digitWidth: Int

    /**
     * Specifies the height of the view.
     */
    val digitHeight: Int

    /**
     * Specifies the drawable or color resource to be used as a background for each character field.
     */
    @AnyRes
    val digitBackground: Int

    /**
     * Specifies the space between characters.
     */
    val digitSpacing: Int

    /**
     * Specifies the size of the characters.
     */
    val digitTextSize: Int

    /**
     * Specifies the resource for the color of the characters.
     */
    @ColorInt
    val digitTextColor: Int

    val digitElevation: Int

    /**
     * Specifies the [AccentType].
     */
    @AccentType
    val accentType: Int

    /**
     * Specifies the width of the accent indicator.
     */
    val accentWidth: Int

    /**
     * Specifies the resource for the accent color.
     */
    @ColorInt
    val accentColor: Int

    /**
     * The string to be displayed for each character.  If set to `null`, the character will not be
     * masked.
     */
    val mask: String?

    // If set to false, will always draw accent color if type is CHARACTER or ALL
    // If set to true, will draw accent color only when focussed.
    val accentRequiresFocus: Boolean

    // Edit text to handle input
    private lateinit var editText: EditText

    private lateinit var internalOnFocusChangeListener: OnFocusChangeListener

    // Focus change listener to send focus events to
    private var _onFocusChangeListener: OnFocusChangeListener? = null

    // Pin entered listener used as a callback for when all digits have been entered
    private var _onPinEnteredListener: OnPinEnteredListener? = null

    init {
        // Get style information
        val array = getContext().obtainStyledAttributes(attrs, R.styleable.PinEntryView)
        digits = array.getInt(R.styleable.PinEntryView_numDigits, 4)
        inputType = array.getInt(R.styleable.PinEntryView_pinInputType, InputType.TYPE_CLASS_NUMBER)
        accentType = array.getInt(R.styleable.PinEntryView_accentType, ACCENT_NONE)

        // Dimensions
        val metrics = resources.displayMetrics
        digitWidth = array.getDimensionPixelSize(
            R.styleable.PinEntryView_digitWidth,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, metrics).toInt()
        )
        digitHeight = array.getDimensionPixelSize(
            R.styleable.PinEntryView_digitHeight,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, metrics).toInt()
        )
        digitSpacing = array.getDimensionPixelSize(
            R.styleable.PinEntryView_digitSpacing,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, metrics).toInt()
        )
        digitTextSize = array.getDimensionPixelSize(
            R.styleable.PinEntryView_digitTextSize,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15f, metrics).toInt()
        )
        accentWidth = array.getDimensionPixelSize(
            R.styleable.PinEntryView_accentWidth,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, metrics).toInt()
        )
        digitElevation = array.getDimensionPixelSize(R.styleable.PinEntryView_digitElevation, 0)

        // Get theme to resolve defaults
        val theme = getContext().theme

        // Background colour, default to android:windowBackground from theme
        val background = TypedValue()
        theme.resolveAttribute(android.R.attr.windowBackground, background, true)
        digitBackground = array.getResourceId(
            R.styleable.PinEntryView_digitBackground,
            background.resourceId
        )

        // Text colour, default to android:textColorPrimary from theme
        val textColor = TypedValue()
        theme.resolveAttribute(android.R.attr.textColorPrimary, textColor, true)
        digitTextColor = array.getColor(
            R.styleable.PinEntryView_digitTextColor,
            if (textColor.resourceId > 0) {
                ContextCompat.getColor(context, textColor.resourceId)
            } else {
                textColor.data
            }
        )

        // Accent colour, default to android:colorAccent from theme
        val accentColor = TypedValue()
        theme.resolveAttribute(android.R.attr.colorAccent, accentColor, true)
        this.accentColor = array.getColor(
            R.styleable.PinEntryView_pinAccentColor,
            if (accentColor.resourceId > 0) {
                ContextCompat.getColor(context, accentColor.resourceId)
            } else {
                accentColor.data
            }
        )

        // Mask character
        mask = array.getString(R.styleable.PinEntryView_mask)

        // Accent shown, default to only when focused
        accentRequiresFocus = array.getBoolean(R.styleable.PinEntryView_accentRequiresFocus, true)

        // Recycle the typed array
        array.recycle()

        setOnLongClickListener {
            showPopupMenu()
            true
        }

        // Add child views
        addViews()

        internalOnFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (v == this) {
                var previousHasNoText = true

                for (i in 0 until digits) {
                    val entryView = getChildAt(i) as DigitView
                    val hasNoText = entryView.text.isNullOrEmpty()

                    Log.i("PinEntryView", "onFocusChange: DigitView $i")
                    Log.i("PinEntryView", "onFocusChange:     text = ${entryView.text?.let { "'$it'" }}")
                    Log.i("PinEntryView", "onFocusChange:     hasNoText = $hasNoText")
                    Log.i("PinEntryView", "onFocusChange:     previousHasNoText = $previousHasNoText")

                    entryView.isActivated = hasFocus &&
                            (((i == 0) && hasNoText) ||
                                    ((i == (digits - 1)) && !previousHasNoText) ||
                                    (!previousHasNoText && hasNoText))

                    previousHasNoText = hasNoText
                }
            }
        }
    }

    /**
     * Save state of the view
     */
    internal class SavedState : BaseSavedState {
        companion object CREATOR : Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }

        var editTextValue: String? = null

        constructor(superState: Parcelable?) : super(superState)

        private constructor(source: Parcel) : super(source) {
            editTextValue = source.readString()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(editTextValue)
        }

        override fun describeContents(): Int = 0
    }

    /**
     * Custom text view that adds a coloured accent when selected
     */
    inner class DigitView @JvmOverloads constructor(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        private val name: String? = null
    ) : TextView(context, attrs, defStyleAttr) {
        /**
         * Paint used to draw accent
         */
        private val paint: Paint = Paint()

        init {
            // Setup paint to keep onDraw as lean as possible
            paint.style = Paint.Style.FILL
            paint.color = accentColor
        }

        override fun onDraw(canvas: Canvas) {
            Log.i("DigitView", "onDraw: name = ${name.let { "'$it'" }}, isActivated = $isActivated")

            super.onDraw(canvas)

            // If selected draw the accent
            if (isSelected || !accentRequiresFocus) {
                canvas.drawRect(
                    0f,
                    (height - accentWidth).toFloat(),
                    width.toFloat(),
                    height.toFloat(),
                    paint
                )
            }
        }

        override fun onTouchEvent(event: MotionEvent?): Boolean {
            if (event!!.action == MotionEvent.ACTION_DOWN) {
                return performClick()
            }

            return super.onTouchEvent(event)
        }

        override fun performClick(): Boolean {
            showKeyboard()
            return super.performClick()
        }
    }

    /**
     * Listener for the [on PIN entered event][OnPinEnteredListener.onPinEntered].
     */
    interface OnPinEnteredListener {
        /**
         * Event that gets triggered once the PIN input length reaches [digits].
         */
        fun onPinEntered(pin: String)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Position the text views
        for (i in 0 until digits) {
            val child = getChildAt(i)
            val left = i * digitWidth + if (i > 0) i * digitSpacing else 0
            child.layout(
                left + paddingLeft + digitElevation,
                paddingTop + digitElevation / 2,
                left + paddingLeft + digitElevation + digitWidth,
                paddingTop + digitElevation / 2 + digitHeight
            )
        }

        // Add the edit text as a 1px wide view to allow it to focus
        getChildAt(digits).layout(0, 0, 1, measuredHeight)
    }

    override fun shouldDelayChildPressedState(): Boolean = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Calculate the size of the view
        val width = digitWidth * digits + digitSpacing * (digits - 1)

        setMeasuredDimension(
            width + paddingLeft + paddingRight + digitElevation * 2,
            digitHeight + paddingTop + paddingBottom + digitElevation * 2
        )

        val height = MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)

        // Measure children
        for (i in 0 until childCount) {
            getChildAt(i).measure(width, height)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val parcelable = super.onSaveInstanceState()
        val savedState = SavedState(parcelable)
        savedState.editTextValue = editText.text.toString()
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        editText.setText(savedState.editTextValue)
        editText.setSelection(savedState.editTextValue?.length ?: 0)
    }

    override fun getOnFocusChangeListener(): OnFocusChangeListener? {
        return _onFocusChangeListener
    }

    override fun setOnFocusChangeListener(l: OnFocusChangeListener?) {
        _onFocusChangeListener = l
    }

    @Suppress("unused")
    fun setOnFocusChangeListener(listener: (View, Boolean) -> Unit) {
        _onFocusChangeListener = OnFocusChangeListener { view, hasFocus ->
            listener(view, hasFocus)
        }
    }

    /**
     * Add a TextWatcher to the EditText
     *
     * @param watcher
     */
    @Suppress("unused")
    fun addTextChangedListener(watcher: TextWatcher?) {
        editText.addTextChangedListener(watcher)
    }

    /**
     * Remove a TextWatcher from the EditText
     *
     * @param watcher
     */
    @Suppress("unused")
    fun removeTextChangedListener(watcher: TextWatcher?) {
        editText.removeTextChangedListener(watcher)
    }

    /**
     * Gets/sets the [PIN entered listener][OnPinEnteredListener].
     */
    @Suppress("unused")
    var onPinEnteredListener: OnPinEnteredListener?
        get() = _onPinEnteredListener
        set(value) {
            _onPinEnteredListener = value
        }

    /**
     * Sets the [PIN entered listener][OnPinEnteredListener].
     */
    @Suppress("unused")
    fun setOnPinEnteredListener(listener: (String) -> Unit) {
        _onPinEnteredListener = object : OnPinEnteredListener {
            override fun onPinEntered(pin: String) {
                listener(pin)
            }
        }
    }

    /**
     * Gets/sets the PIN input.
     */
    @Suppress("unused")
    var text: CharSequence?
        get() = synchronized(editText) { editText.text }
        set(value) {
            synchronized(editText) {
                Log.i("PinEntryView", "setText: value = ${value?.let { "'$it'" }}")

                val txt = if (inputType == InputType.TYPE_CLASS_NUMBER) {
                    value?.replace(Regex("\\D+"), "").let {
                        Log.i("PinEntryView", "setText: text = ${value?.let { t -> "'$t'" }}")

                        val length = it?.length ?: 0

                        if (length > digits) {
                            it?.subSequence(0, digits)
                        } else {
                            it?.subSequence(0, length)
                        }
                    }
                } else {
                    val length = value?.length ?: 0

                    if (length > digits) {
                        value?.subSequence(0, digits)
                    } else {
                        value
                    }
                }

                editText.setText(txt)

                val newText = editText.text
                Log.i("PinEntryView", "setText: newText = ${newText?.let { "'$it'" }}")

                if (!newText.isNullOrEmpty()) {
                    editText.setSelection(min(newText.length, digits))
                } else {
                    editText.setSelection(0)
                }
            }
        }

    /**
     * Clears the PIN input.
     */
    @Suppress("unused")
    fun clearText() {
        synchronized(editText) {
            editText.setText("")
        }
    }

    private fun showPopupMenu() {
        val popupMenu = PopupMenu(context, this)
        popupMenu.inflate(R.menu.pop_up)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.paste -> {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val cbText = clipboard.primaryClip?.getItemAt(0)?.text
                    Log.i("PinEntryView", "paste: cbText = ${cbText?.let { t -> "'$t'" }}")
                    text = cbText
                    true
                }
                R.id.clear -> {
                    clearText()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showKeyboard() {
        // Make sure this view is focused
        editText.requestFocus()

        // Show keyboard
        val inputMethodManager = context
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(editText, 0)
    }

    /**
     * Create views and add them to the view group
     */
    private fun addViews() {
        // Add a digit view for each digit
        for (i in 0 until digits) {
            val digitView = DigitView(context, name = "$id-$i")
            digitView.text = ""
            digitView.width = digitWidth
            digitView.height = digitHeight
            digitView.setBackgroundResource(digitBackground)
            digitView.setTextColor(digitTextColor)
            digitView.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitTextSize.toFloat())
            digitView.gravity = Gravity.CENTER
            digitView.elevation = digitElevation.toFloat()
            addView(digitView)
        }

        // Add an "invisible" edit text to handle input
        editText = EditText(context)
        editText.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        editText.setTextColor(ContextCompat.getColor(context, android.R.color.transparent))
        editText.isCursorVisible = false
        editText.filters = arrayOf<InputFilter>(LengthFilter(digits))
        editText.inputType = inputType
        editText.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
        editText.onFocusChangeListener =
            OnFocusChangeListener { _, hasFocus -> // Update the selected state of the views
                val length = editText.text.length
                for (i in 0 until digits) {
                    getChildAt(i).isSelected = hasFocus && (accentType == ACCENT_ALL ||
                            accentType == ACCENT_CHARACTER && (i == length ||
                            i == digits - 1 && length == digits))
                }

                // Make sure the cursor is at the end
                Log.i("PinEntryView", "onFocusChange: length = $length")
                editText.setSelection(length)

                // Provide focus change events to any listener
                _onFocusChangeListener?.onFocusChange(this@PinEntryView, hasFocus)
                internalOnFocusChangeListener.onFocusChange(this@PinEntryView, hasFocus)
            }
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val length = s.length
                var previousHasNoText = true

                for (i in 0 until digits) {
                    val entryView = getChildAt(i) as DigitView

                    if (s.length > i) {
                        val mask = if (mask.isNullOrEmpty()) {
                            s[i].toString()
                        } else {
                            mask
                        }

                        entryView.text = mask
                    } else {
                        entryView.text = ""
                    }

                    val hasNoText = entryView.text.isNullOrEmpty()

                    entryView.isActivated = ((i == 0) && hasNoText) ||
                            ((i == (digits - 1)) && !previousHasNoText) ||
                            (!previousHasNoText && hasNoText)

                    previousHasNoText = hasNoText

                    if (editText.hasFocus()) {
                        entryView.isSelected = accentType == ACCENT_ALL ||
                                (accentType == ACCENT_CHARACTER) && ((i == length) ||
                                (i == (digits - 1)) && (length == digits))
                    }
                }

                if (length == digits) {
                    _onPinEnteredListener?.onPinEntered(s.toString())
                }
            }
        })
        addView(editText)
    }
}
