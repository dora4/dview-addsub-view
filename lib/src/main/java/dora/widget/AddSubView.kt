package dora.widget

import android.content.Context
import android.content.res.ColorStateList
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import dora.widget.addsubview.R

class AddSubView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), View.OnClickListener, TextWatcher {

    /**
     * 最大购买数量，默认为99。
     */
    private var max = 99

    /**
     * 购买数量。
     */
    private var inputValue = 1

    /**
     * 商品库存，默认最大值。
     */
    private var inventory = 99

    /**
     * 商品最小购买数量，默认值为0。
     */
    private var min = 0

    /**
     * 步长--每次增加的个数，默认是1。
     */
    private var step = 1

    /**
     * 设置改变的位置，默认是0。
     */
    private var position = 0
    private var onWarnListener: OnWarnListener? = null
    private var onChangeValueListener: OnChangeValueListener? = null
    private var etInput: EditText
    private var icPlus: ImageView
    private var icMinus: ImageView

    init {
        val a = getContext().obtainStyledAttributes(attrs, R.styleable.AddSubView)
        val editable = a.getBoolean(R.styleable.AddSubView_dview_asv_editable, true)
        // 左右两面的宽度
        val iconWidth = a.getDimensionPixelSize(R.styleable.AddSubView_dview_asv_iconWidth, -1)
        // 中间内容框的宽度
        val contentWidth =
            a.getDimensionPixelSize(R.styleable.AddSubView_dview_asv_contentWidth, -1)
        // 中间字体的大小
        val contentTextSize =
            a.getDimensionPixelSize(R.styleable.AddSubView_dview_asv_contentTextSize, -1)
        // 中间字体的颜色
        val contentTextColor =
            a.getColor(R.styleable.AddSubView_dview_asv_contentTextColor, -0x1000000)
        // 左面控件的背景
        val leftBackground = a.getDrawable(R.styleable.AddSubView_dview_asv_leftBackground)
        // 右面控件的背景
        val rightBackground = a.getDrawable(R.styleable.AddSubView_dview_asv_rightBackground)
        // 中间控件的背景
        val contentBackground = a.getDrawable(R.styleable.AddSubView_dview_asv_contentBackground)
        // 左面控件的icon
        val leftIcon = a.getDrawable(R.styleable.AddSubView_dview_asv_leftIcon)
        // 右面控件的icon
        val rightIcon = a.getDrawable(R.styleable.AddSubView_dview_asv_rightIcon)
        // 最小值
        min = a.getInt(R.styleable.AddSubView_dview_asv_min, min)
        // 最大值
        max = a.getInt(R.styleable.AddSubView_dview_asv_max, max)
        // 每次递增递减的值
        step = a.getInt(R.styleable.AddSubView_dview_asv_step, step)
        // 资源回收
        a.recycle()
        // 把布局和当前类形成整体
        LayoutInflater.from(context).inflate(R.layout.layout_add_sub, this)
        icPlus = findViewById<View>(R.id.ic_plus) as ImageView
        icMinus = findViewById<View>(R.id.ic_minus) as ImageView
        etInput = findViewById<View>(R.id.et_input) as EditText
        icPlus.setOnClickListener(this)
        icMinus.setOnClickListener(this)
        etInput.setOnClickListener(this)
        etInput.addTextChangedListener(this)
        setEditable(editable)
        // 设置两边按钮的宽度
        if (iconWidth > 0) {
            val textParams = LayoutParams(iconWidth, LayoutParams.MATCH_PARENT)
            icPlus.layoutParams = textParams
            icMinus.layoutParams = textParams
        }
        // 设置中间输入框的宽度
        if (contentWidth > 0) {
            val textParams = LayoutParams(contentWidth, LayoutParams.MATCH_PARENT)
            etInput.layoutParams = textParams
        }
        updateInputFilter()
        etInput.setTextColor(contentTextColor)
        if (contentTextSize > 0) {
            etInput.textSize = contentTextSize.toFloat()
        }
        if (contentBackground != null) {
            etInput.background = contentBackground
        }
        if (leftBackground != null) {
            icMinus.background = leftBackground
        }
        if (rightBackground != null) {
            icPlus.background = rightBackground
        }
        if (leftIcon != null) {
            icMinus.setImageDrawable(leftIcon)
        }
        if (rightIcon != null) {
            icPlus.setImageDrawable(rightIcon)
        }
    }

    private fun updateInputFilter() {
        // 最大长度
        val maxLength = max.toString().length
        // 只允许数字，并限制长度
        etInput.keyListener = DigitsKeyListener.getInstance("0123456789")
        etInput.filters = arrayOf(android.text.InputFilter.LengthFilter(maxLength))
    }

    private fun setEditable(editable: Boolean) {
        if (editable) {
            etInput.isFocusable = true
            etInput.keyListener = DigitsKeyListener()
        } else {
            etInput.isFocusable = false
            etInput.keyListener = null
        }
    }

    override fun onClick(view: View) {
        val id = view.id
        if (id == R.id.ic_plus) {
            // 加
            if (inputValue < max.coerceAtMost(inventory)) {
                inputValue += step
                // 正常添加
                if (etInput.isCursorVisible) {
                    etInput.isCursorVisible = false
                }
                etInput.setText(inputValue.toString())
            } else if (inventory < max) {
                // 库存不足
                warningForInventory()
            } else {
                // 超过最大购买数
                warningForMax()
            }
        } else if (id == R.id.ic_minus) {
            // 减
            if (inputValue > min) {
                inputValue -= step
                if (etInput.isCursorVisible) {
                    etInput.isCursorVisible = false
                }
                etInput.setText(inputValue.toString())
            } else {
                // 低于最小购买数
                warningForMin()
            }
        } else if (id == R.id.et_input) {
            // 输入框
            if (!etInput.isCursorVisible) {
                etInput.isCursorVisible = true
            }
            etInput.setSelection(etInput.text.toString().length)
        }
    }

    /**
     * 低于最小购买数。
     * Warning for buy min.
     */
    private fun warningForMin() {
        icMinus.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                context, R.color.dview_addsub_color_icon_normal
            )
        )
        onWarnListener?.onWarningForMin(min)
    }

    /**
     * 超过的最大购买数限制。
     * Warning for buy max.
     */
    private fun warningForMax() {
        icPlus.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                context, R.color.dview_addsub_color_icon_normal
            )
        )
        onWarnListener?.onWarningForMax(max)
    }

    /**
     * 超过的库存限制。
     * Warning for inventory.
     */
    private fun warningForInventory() {
        onWarnListener?.onWarningForInventory(inventory)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        onNumberInput()
    }

    /**
     * 监听输入的数据变化。
     */
    private fun onNumberInput() {
        // 当前数量
        val count = getNumber()
        if (count < min) {
            // 手动输入
            inputValue = min
            etInput.setText(inputValue.toString())
            etInput.setSelection(etInput.text.toString().length)
            onChangeValueListener?.onChangeValue(inputValue, position)
            return
        }
        val limit = max.coerceAtMost(inventory)
        if (count > limit) {
            if (inventory < max) {
                // 库存不足
                warningForInventory()
            } else {
                // 超过最大购买数
                warningForMax()
            }
        } else if (count == min) {
            warningForMin()
        } else if (count == max) {
            warningForMax()
        } else {
            inputValue = count
            onChangeValueListener?.onChangeValue(inputValue, position)
            icPlus.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    R.color.dview_addsub_color_text
                )
            )
            icMinus.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    R.color.dview_addsub_color_text
                )
            )
        }
    }

    fun setCurrentNumber(currentNumber: Int): AddSubView {
        inputValue = if (currentNumber < min) {
            min
        } else {
            max.coerceAtMost(inventory).coerceAtMost(currentNumber)
        }
        etInput.setText(inputValue.toString())
        etInput.setSelection(etInput.text.toString().length)
        return this
    }

    fun getInventory(): Int {
        return inventory
    }

    fun setInventory(inventory: Int): AddSubView {
        this.inventory = inventory
        return this
    }

    fun setBean(bean: Any?): AddSubView {
        return this
    }

    fun getMax(): Int {
        return max
    }

    fun setMax(max: Int): AddSubView {
        this.max = max
        updateInputFilter()
        return this
    }

    fun setPosition(position: Int): AddSubView {
        this.position = position
        return this
    }

    fun getPosition(): Int {
        return position
    }

    fun setMin(min: Int): AddSubView {
        this.min = min
        return this
    }

    fun setOnWarnListener(listener: OnWarnListener): AddSubView {
        this.onWarnListener = listener
        return this
    }

    fun setOnChangeValueListener(listener: OnChangeValueListener): AddSubView {
        this.onChangeValueListener = listener
        return this
    }

    fun getStep(): Int {
        return step
    }

    fun setStep(step: Int): AddSubView {
        this.step = step
        return this
    }

    override fun afterTextChanged(s: Editable) {}

    /**
     * 得到输入框的数量。
     *
     * @return
     */
    fun getNumber(): Int {
        try {
            return if (TextUtils.isEmpty(etInput.text.toString())) min else etInput.text.toString()
                .toInt()
        } catch (ignore: NumberFormatException) {
        }
        return min
    }

    interface OnWarnListener {

        fun onWarningForInventory(inventory: Int)
        fun onWarningForMax(max: Int)
        fun onWarningForMin(min: Int)
    }

    interface OnChangeValueListener {
        fun onChangeValue(value: Int, position: Int)
    }
}