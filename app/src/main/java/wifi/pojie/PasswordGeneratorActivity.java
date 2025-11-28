package wifi.pojie;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * 密码生成器界面
 */
public class PasswordGeneratorActivity extends AppCompatActivity {
    private static final String TAG = "PasswordGeneratorActivity";
    
    private PasswordGenerator passwordGenerator;
    private SeekBar passwordLengthSeekBar;
    private EditText passwordLengthEdt;
    private TextView generatedPasswordTv;
    private TextView passwordStrengthTv;
    private CheckBox lowercaseCb;
    private CheckBox uppercaseCb;
    private CheckBox digitsCb;
    private CheckBox symbolsCb;
    private CheckBox excludeSimilarCb;
    
    private int passwordLength = 12;
    private boolean includeLowercase = true;
    private boolean includeUppercase = true;
    private boolean includeDigits = true;
    private boolean includeSymbols = true;
    private boolean excludeSimilarChars = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_generator);
        
        // 初始化工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("密码生成器");
        
        // 初始化PasswordGenerator
        passwordGenerator = new PasswordGenerator();
        
        // 初始化视图
        passwordLengthSeekBar = findViewById(R.id.password_length_seekbar);
        passwordLengthEdt = findViewById(R.id.password_length_edt);
        generatedPasswordTv = findViewById(R.id.generated_password_tv);
        passwordStrengthTv = findViewById(R.id.password_strength_tv);
        lowercaseCb = findViewById(R.id.include_lowercase_cb);
        uppercaseCb = findViewById(R.id.include_uppercase_cb);
        digitsCb = findViewById(R.id.include_digits_cb);
        symbolsCb = findViewById(R.id.include_symbols_cb);
        excludeSimilarCb = findViewById(R.id.exclude_similar_cb);
        
        Button generateBtn = findViewById(R.id.generate_btn);
        Button copyBtn = findViewById(R.id.copy_btn);
        Button addToPasswordBookBtn = findViewById(R.id.add_to_password_book_btn);
        FloatingActionButton fabGenerate = findViewById(R.id.fab_generate);
        
        // 设置默认值
        passwordLengthSeekBar.setProgress(passwordLength);
        passwordLengthEdt.setText(String.valueOf(passwordLength));
        lowercaseCb.setChecked(includeLowercase);
        uppercaseCb.setChecked(includeUppercase);
        digitsCb.setChecked(includeDigits);
        symbolsCb.setChecked(includeSymbols);
        excludeSimilarCb.setChecked(excludeSimilarChars);
        
        // 生成初始密码
        generatePassword();
        
        // 设置密码长度滑块监听
        passwordLengthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                passwordLength = Math.max(progress, 4); // 最小密码长度为4
                passwordLengthEdt.setText(String.valueOf(passwordLength));
                generatePassword();
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 设置密码长度输入框监听
        passwordLengthEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int length = Integer.parseInt(s.toString());
                    if (length >= 4 && length <= 128) {
                        passwordLength = length;
                        passwordLengthSeekBar.setProgress(length);
                        generatePassword();
                    }
                } catch (NumberFormatException e) {
                    // 忽略无效输入
                }
            }
        });
        
        // 设置复选框监听
        View.OnClickListener checkboxListener = v -> {
            includeLowercase = lowercaseCb.isChecked();
            includeUppercase = uppercaseCb.isChecked();
            includeDigits = digitsCb.isChecked();
            includeSymbols = symbolsCb.isChecked();
            excludeSimilarChars = excludeSimilarCb.isChecked();
            generatePassword();
        };
        
        lowercaseCb.setOnClickListener(checkboxListener);
        uppercaseCb.setOnClickListener(checkboxListener);
        digitsCb.setOnClickListener(checkboxListener);
        symbolsCb.setOnClickListener(checkboxListener);
        excludeSimilarCb.setOnClickListener(checkboxListener);
        
        // 生成按钮点击事件
        generateBtn.setOnClickListener(v -> generatePassword());
        
        // 复制按钮点击事件
        copyBtn.setOnClickListener(v -> {
            String password = generatedPasswordTv.getText().toString();
            if (!password.isEmpty()) {
                copyToClipboard(password);
                Toast.makeText(this, "密码已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 添加到密码本按钮点击事件
        addToPasswordBookBtn.setOnClickListener(v -> {
            String password = generatedPasswordTv.getText().toString();
            if (!password.isEmpty()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("password", password);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
        
        // 悬浮生成按钮点击事件
        fabGenerate.setOnClickListener(v -> generatePassword());
    }
    
    /**
     * 生成密码
     */
    private void generatePassword() {
        // 确保至少选择一种字符类型
        if (!includeLowercase && !includeUppercase && !includeDigits && !includeSymbols) {
            Toast.makeText(this, "至少选择一种字符类型", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 生成密码
        String password = passwordGenerator.generatePassword(passwordLength, includeLowercase, 
                includeUppercase, includeDigits, includeSymbols, excludeSimilarChars);
        
        // 显示密码
        generatedPasswordTv.setText(password);
        
        // 显示密码强度
        int strength = passwordGenerator.getPasswordStrength(password);
        String strengthText = "强度: ";
        switch (strength) {
            case 1:
                strengthText += "弱";
                passwordStrengthTv.setTextColor(getResources().getColor(R.color.red));
                break;
            case 2:
                strengthText += "较弱";
                passwordStrengthTv.setTextColor(getResources().getColor(R.color.orange));
                break;
            case 3:
                strengthText += "中等";
                passwordStrengthTv.setTextColor(getResources().getColor(R.color.yellow));
                break;
            case 4:
                strengthText += "强";
                passwordStrengthTv.setTextColor(getResources().getColor(R.color.light_green));
                break;
            case 5:
                strengthText += "很强";
                passwordStrengthTv.setTextColor(getResources().getColor(R.color.green));
                break;
        }
        passwordStrengthTv.setText(strengthText);
    }
    
    /**
     * 复制到剪贴板
     */
    private void copyToClipboard(String text) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Generated Password", text);
            clipboard.setPrimaryClip(clip);
        } else {
            // 旧版本处理
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setText(text);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}