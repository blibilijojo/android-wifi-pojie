package wifi.pojie;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * 密码本编辑界面
 */
public class PasswordBookEditActivity extends AppCompatActivity {
    private static final String TAG = "PasswordBookEditActivity";
    
    private PasswordManager passwordManager;
    private EditText passwordBookNameEdt;
    private Spinner categorySpinner;
    private ListView passwordsListView;
    private List<String> categories;
    private List<String> passwords;
    private String currentCategory;
    private String passwordBookId;
    private String mode;
    private PasswordsAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_book_edit);
        
        // 初始化工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // 初始化PasswordManager
        passwordManager = new PasswordManager(this);
        
        // 获取Intent参数
        Intent intent = getIntent();
        mode = intent.getStringExtra("mode");
        passwordBookId = intent.getStringExtra("id");
        
        // 设置标题
        if (mode.equals("create")) {
            getSupportActionBar().setTitle("创建密码本");
        } else {
            getSupportActionBar().setTitle("编辑密码本");
        }
        
        // 初始化视图
        passwordBookNameEdt = findViewById(R.id.password_book_name_edt);
        categorySpinner = findViewById(R.id.category_spinner);
        passwordsListView = findViewById(R.id.passwords_list);
        Button saveBtn = findViewById(R.id.save_btn);
        Button importBtn = findViewById(R.id.import_btn);
        Button exportBtn = findViewById(R.id.export_btn);
        FloatingActionButton fabAddPassword = findViewById(R.id.fab_add_password);
        
        // 加载分类列表
        loadCategories();
        
        // 加载密码本数据
        if (mode.equals("edit")) {
            loadPasswordBookData();
        } else {
            // 创建模式下，初始化为空密码列表
            passwords = new ArrayList<>();
        }
        
        // 设置分类选择器
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = categories.get(position);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentCategory = PasswordManager.DEFAULT_CATEGORY;
            }
        });
        
        // 保存按钮点击事件
        saveBtn.setOnClickListener(v -> savePasswordBook());
        
        // 导入按钮点击事件
        importBtn.setOnClickListener(v -> {
            // 启动文件选择器，导入密码
            Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fileIntent.setType("*/*");
            fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(fileIntent, "选择密码文件"), 1);
        });
        
        // 导出按钮点击事件
        exportBtn.setOnClickListener(v -> {
            if (passwords.isEmpty()) {
                Toast.makeText(this, "密码本为空，无法导出", Toast.LENGTH_SHORT).show();
                return;
            }
            // 这里只实现基本的导出提示，实际导出功能在后面的任务中实现
            Toast.makeText(this, "导出功能开发中...", Toast.LENGTH_SHORT).show();
        });
        
        // 添加密码悬浮按钮点击事件
        fabAddPassword.setOnClickListener(v -> {
            // 这里可以跳转到密码生成界面，或者直接添加一个空密码
            Intent intent1 = new Intent(PasswordBookEditActivity.this, PasswordGeneratorActivity.class);
            startActivityForResult(intent1, 2);
        });
        
        // 初始化密码列表适配器
        adapter = new PasswordsAdapter(passwords);
        passwordsListView.setAdapter(adapter);
    }
    
    /**
     * 加载分类列表
     */
    private void loadCategories() {
        categories = passwordManager.getCategories();
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
        
        // 设置默认分类
        int defaultIndex = categories.indexOf(PasswordManager.DEFAULT_CATEGORY);
        if (defaultIndex != -1) {
            categorySpinner.setSelection(defaultIndex);
            currentCategory = PasswordManager.DEFAULT_CATEGORY;
        }
    }
    
    /**
     * 加载密码本数据
     */
    private void loadPasswordBookData() {
        PasswordManager.PasswordBook passwordBook = passwordManager.getPasswordBook(passwordBookId);
        if (passwordBook != null) {
            passwordBookNameEdt.setText(passwordBook.getName());
            currentCategory = passwordBook.getCategory();
            
            // 设置分类选择器
            int categoryIndex = categories.indexOf(currentCategory);
            if (categoryIndex != -1) {
                categorySpinner.setSelection(categoryIndex);
            }
            
            // 加载密码列表
            passwords = passwordManager.readPasswords(passwordBookId);
        }
    }
    
    /**
     * 保存密码本
     */
    private void savePasswordBook() {
        String name = passwordBookNameEdt.getText().toString().trim();
        
        if (name.isEmpty()) {
            Toast.makeText(this, "密码本名称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentCategory == null) {
            currentCategory = PasswordManager.DEFAULT_CATEGORY;
        }
        
        if (mode.equals("create")) {
            // 创建新密码本
            passwordManager.createPasswordBook(name, currentCategory, passwords);
            Toast.makeText(this, "密码本创建成功", Toast.LENGTH_SHORT).show();
        } else {
            // 更新密码本
            passwordManager.updatePasswordBook(passwordBookId, name, currentCategory, passwords);
            Toast.makeText(this, "密码本更新成功", Toast.LENGTH_SHORT).show();
        }
        
        setResult(RESULT_OK);
        finish();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 2 && resultCode == RESULT_OK) {
            // 从密码生成界面返回，添加生成的密码
            String generatedPassword = data.getStringExtra("password");
            if (generatedPassword != null && !generatedPassword.isEmpty()) {
                passwords.add(generatedPassword);
                adapter.notifyDataSetChanged();
                Toast.makeText(this, "密码已添加", Toast.LENGTH_SHORT).show();
            }
        }
        // 文件导入处理暂时省略
    }
    
    /**
     * 密码列表适配器
     */
    private class PasswordsAdapter extends ArrayAdapter<String> {
        
        public PasswordsAdapter(List<String> passwords) {
            super(PasswordBookEditActivity.this, 0, passwords);
        }
        
        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            String password = getItem(position);
            
            if (convertView == null) {
                convertView = android.view.LayoutInflater.from(getContext())
                        .inflate(R.layout.item_password, parent, false);
            }
            
            // 设置密码文本
            TextView passwordTv = convertView.findViewById(R.id.password_tv);
            Button deleteBtn = convertView.findViewById(R.id.delete_password_btn);
            
            passwordTv.setText(password);
            
            // 删除按钮点击事件
            deleteBtn.setOnClickListener(v -> {
                passwords.remove(position);
                notifyDataSetChanged();
                Toast.makeText(getContext(), "密码已删除", Toast.LENGTH_SHORT).show();
            });
            
            return convertView;
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}