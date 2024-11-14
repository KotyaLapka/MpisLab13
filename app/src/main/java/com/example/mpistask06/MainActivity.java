package com.example.mpistask06;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    // Определение элементов интерфейса
    private Button btnAuthor, btnDownload, btnView, btnDelete;
    private EditText edtJournalId;
    private File pdfFile;
    private TextView txtNoJournal;
    private LinearLayout layoutJournal;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Настройка отступов для системы жестов
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemInsets.left, systemInsets.top, systemInsets.right, systemInsets.bottom);
            return insets;
        });

        // Инициализация элементов
        txtNoJournal = findViewById(R.id.textJournalEmpty);
        layoutJournal = findViewById(R.id.layoutJournalReady);
        progressBar = findViewById(R.id.progressDownload);
        edtJournalId = findViewById(R.id.inputJournalId);
        btnDownload = findViewById(R.id.buttonDownload);
        btnView = findViewById(R.id.buttonView);
        btnDelete = findViewById(R.id.buttonDelete);
        btnAuthor = findViewById(R.id.authorBtn);

        // Настройка видимости
        txtNoJournal.setVisibility(View.VISIBLE);
        layoutJournal.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        // Показ инструкции при первом запуске
        showPopupWindow();

        // Загрузка файла
        btnDownload.setOnClickListener(view -> {
            String journalId = edtJournalId.getText().toString().trim();
            if (journalId.isEmpty()) {
                showAlertDialog("Ошибка", "Заполните все поля.");
                return;
            }

            String url = "https://ntv.ifmo.ru/file/journal/" + journalId + ".pdf";
            String filename = "journal_" + journalId + ".pdf";
            progressBar.setVisibility(View.VISIBLE);
            new DownloadTask(MainActivity.this).execute(url, filename);
        });

        // Открытие загруженного PDF
        btnView.setOnClickListener(view -> {
            if (pdfFile != null && pdfFile.exists()) {
                Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setDataAndType(fileUri, "application/pdf");
                viewIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivity(viewIntent);
                } catch (ActivityNotFoundException e) {
                    showAlertDialog("Ошибка", "Приложение для просмотра PDF не установлено.");
                }
            } else {
                showAlertDialog("Ошибка", "Файл отсутствует.");
            }
        });

        // Удаление файла
        btnDelete.setOnClickListener(view -> {
            if (pdfFile != null && pdfFile.exists()) {
                pdfFile.delete();
                btnView.setEnabled(false);
                btnDelete.setEnabled(false);
                showAlertDialog("Успешно", "Файл удалён.");
                txtNoJournal.setVisibility(View.VISIBLE);
                layoutJournal.setVisibility(View.GONE);
            }
        });

        // Открытие информации об авторе
        btnAuthor.setOnClickListener(view -> showAlertDialog("Автор", getString(R.string.author)));
    }

    // Метод для показа всплывающего окна с инструкцией
    private void showPopupWindow() {
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        if (!preferences.getBoolean("dontShowAgain", false)) {
            edtJournalId.post(() -> {
                View popupView = LayoutInflater.from(this).inflate(R.layout.popup, null);
                PopupWindow popup = new PopupWindow(popupView, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, true);

                CheckBox checkBox = popupView.findViewById(R.id.checkbox);
                Button btnOk = popupView.findViewById(R.id.ok_button);

                btnOk.setOnClickListener(v -> {
                    if (checkBox.isChecked()) {
                        preferences.edit().putBoolean("dontShowAgain", true).apply();
                    }
                    popup.dismiss();
                });

                popup.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);
            });
        }
    }

    // Асинхронная задача для загрузки файла
    private class DownloadTask extends AsyncTask<String, Void, Boolean> {

        private Context context;
        private File folder;

        public DownloadTask(Context ctx) {
            context = ctx;
            folder = new File(context.getExternalFilesDir(null), "Journals");
            if (!folder.exists()) folder.mkdirs();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.VISIBLE);
                txtNoJournal.setVisibility(View.GONE);
            });

            String url = params[0];
            String filename = params[1];
            pdfFile = new File(folder, filename);

            try (InputStream in = new URL(url).openStream();
                 FileOutputStream out = new FileOutputStream(pdfFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (pdfFile.exists()) layoutJournal.setVisibility(View.VISIBLE);
                    else txtNoJournal.setVisibility(View.VISIBLE);
                });
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast.makeText(context, "Файл загружен успешно.", Toast.LENGTH_SHORT).show();
                btnView.setEnabled(true);
                btnDelete.setEnabled(true);
            } else {
                showAlertDialog("Ошибка", "Файл не найден.");
            }
        }
    }

    // Уведомление об ошибках
    private void showAlertDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}
