package com.mikael.ramextreme;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import rikka.shizuku.Shizuku;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int SHIZUKU_PERMISSION_REQUEST = 4101;
    private TextView memoryText, shizukuText, statusText;

    private final Shizuku.OnRequestPermissionResultListener permissionListener =
            (requestCode, grantResult) -> {
                if (requestCode == SHIZUKU_PERMISSION_REQUEST) {
                    updateShizukuStatus();
                    Toast.makeText(this,
                            grantResult == PackageManager.PERMISSION_GRANTED
                                    ? "Permissão Shizuku concedida" : "Permissão Shizuku negada",
                            Toast.LENGTH_LONG).show();
                }
            };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(13, 23, 17));
        getWindow().setNavigationBarColor(Color.rgb(13, 23, 17));
        Shizuku.addRequestPermissionResultListener(permissionListener);
        buildUi();
        refreshMemory();
        updateShizukuStatus();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(22));
        root.setBackgroundColor(Color.rgb(13, 23, 17));

        TextView title = makeText("RAM EXTREME", 27, Color.rgb(151, 245, 174));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title, matchWrap());

        TextView subtitle = makeText("Preparador de desempenho para FCL", 14, Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sub = matchWrap();
        sub.bottomMargin = dp(24);
        root.addView(subtitle, sub);

        memoryText = makeText("Memória: medindo…", 19, Color.WHITE);
        root.addView(memoryText, matchWrap());
        shizukuText = makeText("Shizuku: verificando…", 16, Color.LTGRAY);
        LinearLayout.LayoutParams sp = matchWrap(); sp.topMargin = dp(14);
        root.addView(shizukuText, sp);
        statusText = makeText("Nenhuma otimização foi aplicada.", 14, Color.rgb(190, 205, 193));
        LinearLayout.LayoutParams st = matchWrap(); st.topMargin = dp(18); st.bottomMargin = dp(18);
        root.addView(statusText, st);

        Button permission = makeButton("VERIFICAR / AUTORIZAR SHIZUKU");
        permission.setOnClickListener(v -> requestShizukuPermission());
        root.addView(permission, buttonParams());
        Button refresh = makeButton("ATUALIZAR MEMÓRIA");
        refresh.setOnClickListener(v -> refreshMemory());
        root.addView(refresh, buttonParams());
        Button launch = makeButton("PREPARAR E ABRIR FCL");
        launch.setOnClickListener(v -> prepareAndLaunchFcl());
        root.addView(launch, buttonParams());

        TextView note = makeText("Versão inicial: mede a RAM, verifica Shizuku e tenta localizar o FCL. Não encerra processos do sistema nem promete liberar toda a memória.", 12, Color.GRAY);
        LinearLayout.LayoutParams np = matchWrap(); np.topMargin = dp(20);
        root.addView(note, np);
        setContentView(root);
    }

    private void requestShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            shizukuText.setText("Shizuku desligado. Abra Shizuku e inicie o serviço pela depuração sem fio.");
            return;
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            updateShizukuStatus();
            Toast.makeText(this, "RAM Extreme já está autorizado", Toast.LENGTH_SHORT).show();
        } else {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST);
        }
    }

    private void updateShizukuStatus() {
        if (!Shizuku.pingBinder()) {
            shizukuText.setText("Shizuku: desligado");
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            shizukuText.setText("Shizuku: conectado e autorizado");
        } else {
            shizukuText.setText("Shizuku: conectado, falta autorizar");
        }
    }

    private void refreshMemory() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        manager.getMemoryInfo(info);
        long total = info.totalMem / (1024L * 1024L);
        long available = info.availMem / (1024L * 1024L);
        long used = Math.max(0, total - available);
        memoryText.setText(String.format(Locale.getDefault(),
                "RAM DO SISTEMA\nTotal: %.2f GB\nEm uso aprox.: %.2f GB\nDisponível: %.2f GB",
                total / 1024.0, used / 1024.0, available / 1024.0));
    }

    private void prepareAndLaunchFcl() {
        refreshMemory();
        updateShizukuStatus();
        String pkg = findFclPackage();
        if (pkg == null) {
            statusText.setText("Não encontrei um app chamado FCL. Verifique se está instalado; a próxima versão permitirá informar o pacote manualmente.");
            Toast.makeText(this, "FCL não encontrado", Toast.LENGTH_LONG).show();
            return;
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch == null) {
            statusText.setText("O pacote " + pkg + " não tem uma tela inicial disponível.");
            return;
        }
        statusText.setText("FCL localizado: " + pkg + ". Abrindo. Nenhum processo foi encerrado.");
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launch);
    }

    private String findFclPackage() {
        PackageManager pm = getPackageManager();
        try {
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo app : apps) {
                CharSequence label = pm.getApplicationLabel(app);
                String name = label == null ? "" : label.toString().toLowerCase(Locale.ROOT);
                if ((name.contains("fcl") || name.contains("fold craft"))
                        && pm.getLaunchIntentForPackage(app.packageName) != null) return app.packageName;
            }
        } catch (Exception ignored) { }
        return null;
    }

    private TextView makeText(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private Button makeButton(String value) {
        Button b = new Button(this); b.setText(value); b.setTextColor(Color.WHITE); b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(35, 105, 58))); return b;
    }
    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }
    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = matchWrap(); p.topMargin = dp(10); p.height = dp(54); return p;
    }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        super.onDestroy();
    }
}
