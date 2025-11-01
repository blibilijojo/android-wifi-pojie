package com.pojie.wifipojie;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DictionaryManagerActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST_CODE = 1001;
    private ListView dictionaryListView;
    private Button importButton;
    private ArrayAdapter<String> adapter;
    private List<String> dictionaryFiles = new ArrayList<>();
    private File dictionariesDir;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dictionary_manager);
        setTitle("Dictionary Manager");

        dictionaryListView = findViewById(R.id.listView_dicts);
        importButton = findViewById(R.id.btn_import);

        // Create a dedicated directory for dictionaries in the app's internal storage
        dictionariesDir = new File(getFilesDir(), "dictionaries");
        if (!dictionariesDir.exists()) {
            dictionariesDir.mkdirs();
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dictionaryFiles);
        dictionaryListView.setAdapter(adapter);

        loadDictionaries();

        importButton.setOnClickListener(v -> importDictionary());

        dictionaryListView.setOnItemClickListener((parent, view, position, id) -> {
            String fileName = dictionaryFiles.get(position);
            File selectedFile = new File(dictionariesDir, fileName);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("selected_dictionary_path", selectedFile.getAbsolutePath());
            resultIntent.putExtra("selected_dictionary_name", fileName);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });
    }

    private void loadDictionaries() {
        dictionaryFiles.clear();
        File[] files = dictionariesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                dictionaryFiles.add(file.getName());
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void importDictionary() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Allow any file type
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                // Create a unique name to avoid conflicts
                String fileName = "dict_" + System.currentTimeMillis() + ".txt"; 
                File destFile = new File(dictionariesDir, fileName);

                try (InputStream is = getContentResolver().openInputStream(uri);
                     OutputStream os = new FileOutputStream(destFile)) {
                    
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        os.write(buffer, 0, length);
                    }
                    Toast.makeText(this, "Import successful: " + fileName, Toast.LENGTH_SHORT).show();
                    loadDictionaries(); // Refresh the list
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Import failed.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
