package wifi.pojie;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * 密码本分类管理界面
 */
public class PasswordBookCategoryActivity extends AppCompatActivity {
    private static final String TAG = "PasswordBookCategoryActivity";
    
    private PasswordManager passwordManager;
    private ListView categoryListView;
    private EditText newCategoryEdt;
    private List<String> categories;
    private ArrayAdapter<String> adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_book_category);
        
        // 初始化工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("分类管理");
        
        // 初始化PasswordManager
        passwordManager = new PasswordManager(this);
        
        // 初始化视图
        categoryListView = findViewById(R.id.category_list);
        newCategoryEdt = findViewById(R.id.new_category_edt);
        Button addCategoryBtn = findViewById(R.id.add_category_btn);
        FloatingActionButton fabAddCategory = findViewById(R.id.fab_add_category);
        
        // 加载分类列表
        loadCategories();
        
        // 添加分类按钮点击事件
        addCategoryBtn.setOnClickListener(v -> addCategory());
        
        // 悬浮添加按钮点击事件
        fabAddCategory.setOnClickListener(v -> addCategory());
        
        // 分类列表项长按事件
        categoryListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String category = categories.get(position);
                // 不能删除默认分类
                if (category.equals(PasswordManager.DEFAULT_CATEGORY)) {
                    Toast.makeText(PasswordBookCategoryActivity.this, "不能删除默认分类", Toast.LENGTH_SHORT).show();
                    return true;
                }
                
                // 询问用户是否删除
                showDeleteDialog(category);
                return true;
            }
        });
    }
    
    /**
     * 加载分类列表
     */
    private void loadCategories() {
        categories = passwordManager.getCategories();
        adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_list_item_1, categories);
        categoryListView.setAdapter(adapter);
    }
    
    /**
     * 添加分类
     */
    private void addCategory() {
        String newCategory = newCategoryEdt.getText().toString().trim();
        
        if (newCategory.isEmpty()) {
            Toast.makeText(this, "分类名称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (categories.contains(newCategory)) {
            Toast.makeText(this, "分类已存在", Toast.LENGTH_SHORT).show();
            return;
        }
        
        passwordManager.createCategory(newCategory);
        categories.add(newCategory);
        adapter.notifyDataSetChanged();
        newCategoryEdt.setText("");
        Toast.makeText(this, "分类添加成功", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 显示删除对话框
     */
    private void showDeleteDialog(String category) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("删除分类")
                .setMessage("确定要删除分类 '" + category + "' 吗？该分类下的密码本将被移动到默认分类。")
                .setPositiveButton("确定", (dialog, which) -> {
                    passwordManager.deleteCategory(category);
                    categories.remove(category);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "分类删除成功", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}