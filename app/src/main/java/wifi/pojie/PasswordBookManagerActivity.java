package wifi.pojie;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
 * 密码本管理主界面
 */
public class PasswordBookManagerActivity extends AppCompatActivity {
    private static final String TAG = "PasswordBookManagerActivity";
    
    private PasswordManager passwordManager;
    private ListView passwordBookListView;
    private Spinner categorySpinner;
    private List<PasswordManager.PasswordBook> passwordBookList;
    private PasswordBookAdapter adapter;
    private List<String> categories;
    private String currentCategory;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_book_manager);
        
        // 初始化工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("密码本管理");
        
        // 初始化PasswordManager
        passwordManager = new PasswordManager(this);
        
        // 初始化视图
        passwordBookListView = findViewById(R.id.password_book_list);
        categorySpinner = findViewById(R.id.category_spinner);
        Button addPasswordBookBtn = findViewById(R.id.add_password_book_btn);
        Button manageCategoriesBtn = findViewById(R.id.manage_categories_btn);
        FloatingActionButton fabGeneratePassword = findViewById(R.id.fab_generate_password);
        
        // 加载分类列表
        loadCategories();
        
        // 设置分类选择器
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = categories.get(position);
                loadPasswordBooks();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentCategory = PasswordManager.DEFAULT_CATEGORY;
                loadPasswordBooks();
            }
        });
        
        // 添加密码本按钮点击事件
        addPasswordBookBtn.setOnClickListener(v -> {
            Intent intent = new Intent(PasswordBookManagerActivity.this, PasswordBookEditActivity.class);
            intent.putExtra("mode", "create");
            startActivityForResult(intent, 1);
        });
        
        // 管理分类按钮点击事件
        manageCategoriesBtn.setOnClickListener(v -> {
            Intent intent = new Intent(PasswordBookManagerActivity.this, PasswordBookCategoryActivity.class);
            startActivityForResult(intent, 2);
        });
        
        // 生成密码悬浮按钮点击事件
        fabGeneratePassword.setOnClickListener(v -> {
            Intent intent = new Intent(PasswordBookManagerActivity.this, PasswordGeneratorActivity.class);
            startActivity(intent);
        });
        
        // 设置密码本列表项点击事件
        passwordBookListView.setOnItemClickListener((parent, view, position, id) -> {
            PasswordManager.PasswordBook passwordBook = passwordBookList.get(position);
            Intent intent = new Intent(PasswordBookManagerActivity.this, PasswordBookEditActivity.class);
            intent.putExtra("mode", "edit");
            intent.putExtra("id", passwordBook.getId());
            startActivityForResult(intent, 1);
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 || requestCode == 2) {
            // 重新加载数据
            loadCategories();
            loadPasswordBooks();
        }
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
        
        // 设置当前分类为默认分类
        int defaultIndex = categories.indexOf(PasswordManager.DEFAULT_CATEGORY);
        if (defaultIndex != -1) {
            categorySpinner.setSelection(defaultIndex);
            currentCategory = PasswordManager.DEFAULT_CATEGORY;
        }
    }
    
    /**
     * 加载密码本列表
     */
    private void loadPasswordBooks() {
        if (currentCategory == null) {
            currentCategory = PasswordManager.DEFAULT_CATEGORY;
        }
        
        passwordBookList = passwordManager.getPasswordBooksByCategory(currentCategory);
        adapter = new PasswordBookAdapter(this, passwordBookList, passwordManager);
        passwordBookListView.setAdapter(adapter);
    }
    
    /**
     * 密码本适配器
     */
    private static class PasswordBookAdapter extends ArrayAdapter<PasswordManager.PasswordBook> {
        private final PasswordManager passwordManager;
        
        public PasswordBookAdapter(PasswordBookManagerActivity context, 
                                   List<PasswordManager.PasswordBook> passwordBooks, 
                                   PasswordManager passwordManager) {
            super(context, 0, passwordBooks);
            this.passwordManager = passwordManager;
        }
        
        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            PasswordManager.PasswordBook passwordBook = getItem(position);
            
            if (convertView == null) {
                convertView = android.view.LayoutInflater.from(getContext())
                        .inflate(R.layout.item_password_book, parent, false);
            }
            
            // 设置密码本信息
            TextView nameTextView = convertView.findViewById(R.id.password_book_name);
            TextView infoTextView = convertView.findViewById(R.id.password_book_info);
            Button deleteBtn = convertView.findViewById(R.id.delete_password_book_btn);
            
            nameTextView.setText(passwordBook.getName());
            infoTextView.setText(String.format("密码数量: %d | 创建时间: %s", 
                    passwordBook.getPasswordCount(), 
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(passwordBook.getCreateTime()))));
            
            // 删除按钮点击事件
            deleteBtn.setOnClickListener(v -> {
                passwordManager.deletePasswordBook(passwordBook.getId());
                remove(passwordBook);
                notifyDataSetChanged();
                Toast.makeText(getContext(), "密码本已删除", Toast.LENGTH_SHORT).show();
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