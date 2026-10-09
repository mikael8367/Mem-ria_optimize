package com.mikael.ramextreme;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int SHIZUKU_PERMISSION_REQUEST = 4101;
    private static final String FCL_PACKAGE = "com.tungsten.fcl";

    private TextView memoryText;
    private TextView shizukuText;
    private TextView statusText;
    private LinearLayout appList;
    private Button permissionButton, refreshButton, launchButton;
    private final List<CheckBox> appChecks = new ArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private final Shizuku.OnRequestPermissionResultListener permissionListener =
            (requestCode, grantResult) -> {
                if (requestCode == SHIZUKU_PERMISSION_REQUEST) {
                    updateShizukuStatus();
                    Toast.makeText(this,
                            grantResult == PackageManager.PERMISSION_GRANTED
                                    ? "Permissão Shizuku concedida. Toque em ATIVAR MODO EXTREMO."
                                    : "Permissão Shizuku negada",
                            Toast.LENGTH_LONG).show();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(13, 23, 17));
        getWindow().setNavigationBarColor(Color.rgb(13, 23, 17));
        Shizuku.addRequestPermissionResultListener(permissionListener);
        buildUi();
        refreshMemory();
        updateShizukuStatus();
        loadUserApps();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        root.setBackgroundColor(Color.rgb(13, 23, 17));

        TextView title = makeText("RAM EXTREME", 27, Color.rgb(151, 245, 174));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title, matchWrap());

        TextView subtitle = makeText("Preparador de desempenho para FCL", 14, Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sub = matchWrap();
        sub.bottomMargin = dp(20);
        root.addView(subtitle, sub);

        memoryText = makeText("RAM: medindo…", 17, Color.WHITE);
        root.addView(memoryText, matchWrap());

        shizukuText = makeText("Shizuku: verificando…", 14, Color.LTGRAY);
        LinearLayout.LayoutParams sp = matchWrap();
        sp.topMargin = dp(10);
        root.addView(shizukuText, sp);

        TextView listTitle = makeText("APLICATIVOS PARA FECHAR ANTES DE JOGAR", 14,
                Color.rgb(151, 245, 174));
        LinearLayout.LayoutParams lt = matchWrap();
        lt.topMargin = dp(22);
        lt.bottomMargin = dp(6);
        root.addView(listTitle, lt);

        TextView hint = makeText("Marque somente apps que você aceita interromper. O FCL e apps do sistema são excluídos.", 12, Color.LTGRAY);
        root.addView(hint, matchWrap());

        appList = new LinearLayout(this);
        appList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = matchWrap();
        listParams.topMargin = dp(8);
        root.addView(appList, listParams);

        statusText = makeText("Nenhum app foi fechado. Escolha os apps e ative o modo.", 13,
                Color.rgb(190, 205, 193));
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.topMargin = dp(16);
        statusParams.bottomMargin = dp(8);
        root.addView(statusText, statusParams);

        permissionButton = makeButton("VERIFICAR / AUTORIZAR SHIZUKU");
        permissionButton.setOnClickListener(v -> requestShizukuPermission());
        root.addView(permissionButton, buttonParams());

        refreshButton = makeButton("ATUALIZAR MEMÓRIA");
        refreshButton.setOnClickListener(v -> refreshMemory());
        root.addView(refreshButton, buttonParams());

        launchButton = makeButton("ATIVAR MODO EXTREMO + ABRIR FCL");
        launchButton.setOnClickListener(v -> activateExtremeMode());
        root.addView(launchButton, buttonParams());

        TextView note = makeText("O modo extremo usa o Shizuku para solicitar force-stop somente nos apps comuns que você marcou. Isso pode fechar tarefas não salvas. A RAM livre e o FPS variam; não há garantia de aumento.", 12, Color.GRAY);
        LinearLayout.LayoutParams np = matchWrap();
        np.topMargin = dp(18);
        root.addView(note, np);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void loadUserApps() {
        appList.removeAllViews();
        appChecks.clear();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<ApplicationInfo> userApps = new ArrayList<>();

        for (ApplicationInfo app : apps) {
            if (app.packageName.equals(getPackageName()) || app.packageName.equals(FCL_PACKAGE)) continue;
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            if (pm.getLaunchIntentForPackage(app.packageName) == null) continue;
            userApps.add(app);
        }

        Collections.sort(userApps, new Comparator<ApplicationInfo>() {
            @Override public int compare(ApplicationInfo a, ApplicationInfo b) {
                String la = String.valueOf(pm.getApplicationLabel(a));
                String lb = String.valueOf(pm.getApplicationLabel(b));
                return la.compareToIgnoreCase(lb);
            }
        });

        if (userApps.isEmpty()) {
            appList.addView(makeText("Nenhum aplicativo comum encontrado.", 13, Color.LTGRAY), matchWrap());
            return;
        }

        for (ApplicationInfo app : userApps) {
            String label = String.valueOf(pm.getApplicationLabel(app));
            CheckBox check = new CheckBox(this);
            check.setText(label + "\n" + app.packageName);
            check.setTextColor(Color.WHITE);
            check.setTextSize(13);
            check.setTag(app.packageName);
            check.setPadding(dp(2), dp(4), dp(2), dp(4));
            appList.addView(check, matchWrap());
            appChecks.add(check);
        }
    }

    private void requestShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            shizukuText.setText("Shizuku desligado. Abra Shizuku e inicie o serviço por Depuração sem fio.");
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

    private void activateExtremeMode() {
        refreshMemory();
        if (!Shizuku.pingBinder()) {
            statusText.setText("Shizuku não está iniciado. Inicie-o e tente novamente; o modo extremo precisa dele para fechar apps.");
            updateShizukuStatus();
            return;
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            statusText.setText("Autorize o RAM Extreme no diálogo do Shizuku e toque novamente no botão.");
            requestShizukuPermission();
            return;
        }

        final List<String> selectedPackages = new ArrayList<>();
        for (CheckBox check : appChecks) {
            if (check.isChecked()) {
                Object tag = check.getTag();
                if (tag instanceof String) {
                    String pkg = (String) tag;
                    if (pkg.matches("[A-Za-z0-9_.]+")
                            && !pkg.equals(FCL_PACKAGE)
                            && !pkg.equals(getPackageName())) {
                        selectedPackages.add(pkg);
                    }
                }
            }
        }

        final Intent fclIntent = getPackageManager().getLaunchIntentForPackage(FCL_PACKAGE);
        if (fclIntent == null) {
            statusText.setText("Não encontrei o pacote " + FCL_PACKAGE + ". Verifique se o FCL está instalado.");
            Toast.makeText(this, "FCL não encontrado: " + FCL_PACKAGE, Toast.LENGTH_LONG).show();
            return;
        }

        statusText.setText("Modo extremo: fechando " + selectedPackages.size()
                + " aplicativo(s) selecionado(s)…");
        setButtonsEnabled(false);

        worker.execute(() -> {
            int success = 0;
            List<String> errors = new ArrayList<>();
            for (String pkg : selectedPackages) {
                try {
                    String command = "am force-stop --user current " + pkg + " 2>&1";
                    Process process = Shizuku.newProcess(new String[]{"sh", "-c", command}, null, null);
                    String output = readProcessOutput(process.getInputStream());
                    String error = readProcessOutput(process.getErrorStream());
                    int exit = process.waitFor();
                    if (exit == 0) {
                        success++;
                    } else {
                        errors.add(pkg + (error.isEmpty() ? "" : ": " + error.trim())
                                + (output.isEmpty() ? "" : " " + output.trim()));
                    }
                    process.destroy();
                } catch (Exception e) {
                    errors.add(pkg + ": " + e.getMessage());
                }
            }

            final int closed = success;
            final int requested = selectedPackages.size();
            final String errorSummary = errors.isEmpty() ? "" : "\nFalhas: "
                    + String.join("; ", errors.subList(0, Math.min(3, errors.size())));
            runOnUiThread(() -> {
                setButtonsEnabled(true);
                refreshMemory();
                statusText.setText("Modo extremo concluído: " + closed + "/" + requested
                        + " app(s) interrompido(s)." + errorSummary + "\nAbrindo FCL…");
                try {
                    fclIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(fclIntent);
                } catch (Exception e) {
                    statusText.setText("Apps interrompidos: " + closed + ". Falha ao abrir FCL: " + e.getMessage());
                }
            });
        });
    }

    private String readProcessOutput(InputStream stream) {
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        if (permissionButton != null) permissionButton.setEnabled(enabled);
        if (refreshButton != null) refreshButton.setEnabled(enabled);
        if (launchButton != null) launchButton.setEnabled(enabled);
    }

    private TextView makeText(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private Button makeButton(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(35, 105, 58)));
        return b;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = matchWrap();
        p.topMargin = dp(8);
        p.height = dp(52);
        return p;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        worker.shutdownNow();
        super.onDestroy();
    }
}
