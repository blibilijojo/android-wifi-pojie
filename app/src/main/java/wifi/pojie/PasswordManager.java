package wifi.pojie;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 密码本管理器，用于管理密码本的创建、编辑、删除和分类
 */
public class PasswordManager {
    private static final String TAG = "PasswordManager";
    private static final String PREFS_NAME = "password_manager";
    private static final String KEY_PASSWORD_BOOKS = "password_books";
    private static final String KEY_CATEGORIES = "categories";
    private static final String DEFAULT_CATEGORY = "默认分类";

    private final SharedPreferences prefs;
    private final Context context;
    private final File passwordBooksDir;

    public PasswordManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // 创建密码本存储目录
        File appDir = new File(Environment.getExternalStorageDirectory(), "WifiPojie");
        this.passwordBooksDir = new File(appDir, "PasswordBooks");
        if (!this.passwordBooksDir.exists()) {
            this.passwordBooksDir.mkdirs();
        }
        
        // 初始化默认分类
        initDefaultCategory();
    }

    /**
     * 初始化默认分类
     */
    private void initDefaultCategory() {
        List<String> categories = getCategories();
        if (!categories.contains(DEFAULT_CATEGORY)) {
            categories.add(DEFAULT_CATEGORY);
            saveCategories(categories);
        }
    }

    /**
     * 获取所有分类
     */
    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        String categoriesJson = prefs.getString(KEY_CATEGORIES, "[]");
        try {
            JSONArray jsonArray = new JSONArray(categoriesJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                categories.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing categories", e);
        }
        return categories;
    }

    /**
     * 保存分类列表
     */
    private void saveCategories(List<String> categories) {
        JSONArray jsonArray = new JSONArray(categories);
        prefs.edit().putString(KEY_CATEGORIES, jsonArray.toString()).apply();
    }

    /**
     * 添加新分类
     */
    public boolean addCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return false;
        }
        
        List<String> categories = getCategories();
        if (categories.contains(categoryName)) {
            return false; // 分类已存在
        }
        
        categories.add(categoryName);
        saveCategories(categories);
        return true;
    }

    /**
     * 删除分类
     */
    public boolean deleteCategory(String categoryName) {
        if (categoryName == null || DEFAULT_CATEGORY.equals(categoryName)) {
            return false; // 不能删除默认分类
        }
        
        List<String> categories = getCategories();
        if (!categories.contains(categoryName)) {
            return false;
        }
        
        // 将该分类下的密码本移动到默认分类
        List<PasswordBook> passwordBooks = getPasswordBooksByCategory(categoryName);
        for (PasswordBook book : passwordBooks) {
            book.setCategory(DEFAULT_CATEGORY);
            updatePasswordBook(book);
        }
        
        categories.remove(categoryName);
        saveCategories(categories);
        return true;
    }

    /**
     * 修改分类名称
     */
    public boolean renameCategory(String oldName, String newName) {
        if (oldName == null || newName == null || DEFAULT_CATEGORY.equals(oldName)) {
            return false; // 不能修改默认分类
        }
        
        List<String> categories = getCategories();
        if (!categories.contains(oldName) || categories.contains(newName)) {
            return false;
        }
        
        // 更新该分类下的所有密码本
        List<PasswordBook> passwordBooks = getPasswordBooksByCategory(oldName);
        for (PasswordBook book : passwordBooks) {
            book.setCategory(newName);
            updatePasswordBook(book);
        }
        
        // 更新分类列表
        int index = categories.indexOf(oldName);
        categories.set(index, newName);
        saveCategories(categories);
        return true;
    }

    /**
     * 获取所有密码本
     */
    public List<PasswordBook> getAllPasswordBooks() {
        List<PasswordBook> passwordBooks = new ArrayList<>();
        String passwordBooksJson = prefs.getString(KEY_PASSWORD_BOOKS, "[]");
        try {
            JSONArray jsonArray = new JSONArray(passwordBooksJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                passwordBooks.add(PasswordBook.fromJson(jsonObject));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing password books", e);
        }
        return passwordBooks;
    }

    /**
     * 根据分类获取密码本
     */
    public List<PasswordBook> getPasswordBooksByCategory(String category) {
        List<PasswordBook> result = new ArrayList<>();
        List<PasswordBook> allBooks = getAllPasswordBooks();
        for (PasswordBook book : allBooks) {
            if (category.equals(book.getCategory())) {
                result.add(book);
            }
        }
        return result;
    }

    /**
     * 根据ID获取密码本
     */
    public PasswordBook getPasswordBookById(String id) {
        List<PasswordBook> allBooks = getAllPasswordBooks();
        for (PasswordBook book : allBooks) {
            if (id.equals(book.getId())) {
                return book;
            }
        }
        return null;
    }

    /**
     * 保存密码本列表
     */
    private void savePasswordBooks(List<PasswordBook> passwordBooks) {
        JSONArray jsonArray = new JSONArray();
        for (PasswordBook book : passwordBooks) {
            jsonArray.put(book.toJson());
        }
        prefs.edit().putString(KEY_PASSWORD_BOOKS, jsonArray.toString()).apply();
    }

    /**
     * 创建新密码本
     */
    public boolean createPasswordBook(String name, String category, List<String> passwords) {
        if (name == null || name.trim().isEmpty() || category == null) {
            return false;
        }
        
        // 检查名称是否已存在
        List<PasswordBook> existingBooks = getAllPasswordBooks();
        for (PasswordBook book : existingBooks) {
            if (name.equals(book.getName())) {
                return false; // 名称已存在
            }
        }
        
        // 创建密码本文件
        File passwordBookFile = new File(passwordBooksDir, System.currentTimeMillis() + ".txt");
        if (!writePasswordsToFile(passwordBookFile, passwords)) {
            return false;
        }
        
        // 创建密码本对象
        PasswordBook passwordBook = new PasswordBook();
        passwordBook.setId(System.currentTimeMillis() + "");
        passwordBook.setName(name);
        passwordBook.setCategory(category);
        passwordBook.setFilePath(passwordBookFile.getAbsolutePath());
        passwordBook.setCreateTime(System.currentTimeMillis());
        passwordBook.setUpdateTime(System.currentTimeMillis());
        passwordBook.setPasswordCount(passwords.size());
        
        // 添加到列表并保存
        existingBooks.add(passwordBook);
        savePasswordBooks(existingBooks);
        return true;
    }

    /**
     * 更新密码本
     */
    public boolean updatePasswordBook(PasswordBook passwordBook) {
        if (passwordBook == null || passwordBook.getId() == null) {
            return false;
        }
        
        List<PasswordBook> existingBooks = getAllPasswordBooks();
        for (int i = 0; i < existingBooks.size(); i++) {
            if (passwordBook.getId().equals(existingBooks.get(i).getId())) {
                // 更新修改时间
                passwordBook.setUpdateTime(System.currentTimeMillis());
                existingBooks.set(i, passwordBook);
                savePasswordBooks(existingBooks);
                return true;
            }
        }
        return false;
    }

    /**
     * 更新密码本内容
     */
    public boolean updatePasswordBookContent(String id, List<String> passwords) {
        PasswordBook passwordBook = getPasswordBookById(id);
        if (passwordBook == null) {
            return false;
        }
        
        // 写入新内容
        File passwordBookFile = new File(passwordBook.getFilePath());
        if (!writePasswordsToFile(passwordBookFile, passwords)) {
            return false;
        }
        
        // 更新密码本信息
        passwordBook.setPasswordCount(passwords.size());
        passwordBook.setUpdateTime(System.currentTimeMillis());
        return updatePasswordBook(passwordBook);
    }

    /**
     * 删除密码本
     */
    public boolean deletePasswordBook(String id) {
        List<PasswordBook> existingBooks = getAllPasswordBooks();
        for (int i = 0; i < existingBooks.size(); i++) {
            if (id.equals(existingBooks.get(i).getId())) {
                // 删除文件
                File passwordBookFile = new File(existingBooks.get(i).getFilePath());
                if (passwordBookFile.exists()) {
                    passwordBookFile.delete();
                }
                
                // 从列表中移除并保存
                existingBooks.remove(i);
                savePasswordBooks(existingBooks);
                return true;
            }
        }
        return false;
    }

    /**
     * 重命名密码本
     */
    public boolean renamePasswordBook(String id, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return false;
        }
        
        // 检查新名称是否已存在
        List<PasswordBook> existingBooks = getAllPasswordBooks();
        for (PasswordBook book : existingBooks) {
            if (newName.equals(book.getName()) && !id.equals(book.getId())) {
                return false; // 名称已存在
            }
        }
        
        // 更新名称
        PasswordBook passwordBook = getPasswordBookById(id);
        if (passwordBook == null) {
            return false;
        }
        
        passwordBook.setName(newName);
        passwordBook.setUpdateTime(System.currentTimeMillis());
        return updatePasswordBook(passwordBook);
    }

    /**
     * 更改密码本分类
     */
    public boolean changePasswordBookCategory(String id, String newCategory) {
        PasswordBook passwordBook = getPasswordBookById(id);
        if (passwordBook == null) {
            return false;
        }
        
        passwordBook.setCategory(newCategory);
        passwordBook.setUpdateTime(System.currentTimeMillis());
        return updatePasswordBook(passwordBook);
    }

    /**
     * 从文件中读取密码列表
     */
    public List<String> readPasswordsFromFile(String filePath) {
        List<String> passwords = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) {
            return passwords;
        }
        
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int len;
            while ((len = isr.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            
            String content = sb.toString();
            String[] lines = content.split("\\n");
            for (String line : lines) {
                String password = line.trim();
                if (!password.isEmpty()) {
                    passwords.add(password);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading passwords from file: " + filePath, e);
        }
        
        return passwords;
    }

    /**
     * 将密码列表写入文件
     */
    private boolean writePasswordsToFile(File file, List<String> passwords) {
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            for (String password : passwords) {
                osw.write(password);
                osw.write("\n");
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error writing passwords to file: " + file.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 从TXT文件导入密码本
     */
    public boolean importFromTxt(File file, String name, String category) {
        List<String> passwords = readPasswordsFromFile(file.getAbsolutePath());
        return createPasswordBook(name, category, passwords);
    }

    /**
     * 从CSV文件导入密码本
     */
    public boolean importFromCsv(File file, String name, String category) {
        List<String> passwords = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 0) {
                    String password = parts[0].trim();
                    if (!password.isEmpty()) {
                        passwords.add(password);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading CSV file: " + file.getAbsolutePath(), e);
            return false;
        }
        
        return createPasswordBook(name, category, passwords);
    }

    /**
     * 从ZIP文件导入密码本（批量）
     */
    public boolean importFromZip(File zipFile) {
        try (FileInputStream fis = new FileInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis, StandardCharsets.UTF_8)) {
            
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String fileName = entry.getName();
                    // 从文件名中提取密码本名称（去除扩展名）
                    String name = fileName.substring(0, fileName.lastIndexOf('.'));
                    
                    // 读取密码内容
                    StringBuilder sb = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        sb.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
                    }
                    
                    // 解析密码列表
                    List<String> passwords = new ArrayList<>();
                    String[] lines = sb.toString().split("\\n");
                    for (String line : lines) {
                        String password = line.trim();
                        if (!password.isEmpty()) {
                            passwords.add(password);
                        }
                    }
                    
                    // 创建密码本（使用默认分类）
                    createPasswordBook(name, DEFAULT_CATEGORY, passwords);
                }
                zis.closeEntry();
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error importing from ZIP file: " + zipFile.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 导出单个密码本为TXT文件
     */
    public boolean exportToTxt(PasswordBook passwordBook, File outputFile) {
        List<String> passwords = readPasswordsFromFile(passwordBook.getFilePath());
        return writePasswordsToFile(outputFile, passwords);
    }

    /**
     * 导出单个密码本为CSV文件
     */
    public boolean exportToCsv(PasswordBook passwordBook, File outputFile) {
        List<String> passwords = readPasswordsFromFile(passwordBook.getFilePath());
        
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw)) {
            
            // 写入CSV头
            bw.write("password");
            bw.newLine();
            
            // 写入密码数据
            for (String password : passwords) {
                bw.write(password);
                bw.newLine();
            }
            
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error exporting to CSV file: " + outputFile.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 导出所有密码本为ZIP文件
     */
    public boolean exportAllToZip(File outputFile) {
        List<PasswordBook> allBooks = getAllPasswordBooks();
        
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {
            
            for (PasswordBook book : allBooks) {
                List<String> passwords = readPasswordsFromFile(book.getFilePath());
                
                // 创建ZIP条目
                String entryName = book.getName() + ".txt";
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                
                // 写入密码内容
                for (String password : passwords) {
                    zos.write(password.getBytes(StandardCharsets.UTF_8));
                    zos.write("\n".getBytes(StandardCharsets.UTF_8));
                }
                
                zos.closeEntry();
            }
            
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error exporting all to ZIP file: " + outputFile.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 导出指定密码本列表为ZIP文件
     */
    public boolean exportToZip(List<PasswordBook> passwordBooks, File outputFile) {
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {
            
            for (PasswordBook book : passwordBooks) {
                List<String> passwords = readPasswordsFromFile(book.getFilePath());
                
                // 创建ZIP条目
                String entryName = book.getName() + ".txt";
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                
                // 写入密码内容
                for (String password : passwords) {
                    zos.write(password.getBytes(StandardCharsets.UTF_8));
                    zos.write("\n".getBytes(StandardCharsets.UTF_8));
                }
                
                zos.closeEntry();
            }
            
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error exporting to ZIP file: " + outputFile.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 密码本模型类
     */
    public static class PasswordBook {
        private String id;
        private String name;
        private String category;
        private String filePath;
        private long createTime;
        private long updateTime;
        private int passwordCount;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public long getCreateTime() { return createTime; }
        public void setCreateTime(long createTime) { this.createTime = createTime; }
        public long getUpdateTime() { return updateTime; }
        public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
        public int getPasswordCount() { return passwordCount; }
        public void setPasswordCount(int passwordCount) { this.passwordCount = passwordCount; }

        /**
         * 转换为JSON对象
         */
        public JSONObject toJson() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("id", id);
                jsonObject.put("name", name);
                jsonObject.put("category", category);
                jsonObject.put("filePath", filePath);
                jsonObject.put("createTime", createTime);
                jsonObject.put("updateTime", updateTime);
                jsonObject.put("passwordCount", passwordCount);
            } catch (JSONException e) {
                Log.e(TAG, "Error converting PasswordBook to JSON", e);
            }
            return jsonObject;
        }

        /**
         * 从JSON对象转换
         */
        public static PasswordBook fromJson(JSONObject jsonObject) {
            PasswordBook passwordBook = new PasswordBook();
            try {
                passwordBook.setId(jsonObject.getString("id"));
                passwordBook.setName(jsonObject.getString("name"));
                passwordBook.setCategory(jsonObject.getString("category"));
                passwordBook.setFilePath(jsonObject.getString("filePath"));
                passwordBook.setCreateTime(jsonObject.getLong("createTime"));
                passwordBook.setUpdateTime(jsonObject.getLong("updateTime"));
                passwordBook.setPasswordCount(jsonObject.getInt("passwordCount"));
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing PasswordBook from JSON", e);
            }
            return passwordBook;
        }
    }
}