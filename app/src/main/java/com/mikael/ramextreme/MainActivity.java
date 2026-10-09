package com.mikael.ramextreme;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.DialogInterface;
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
    private LinearLayout appList, systemAppList;
    private Button permissionButton, refreshButton, launchButton, animationsButton, processLimitButton, restoreButton, closeAllButton, closeSelectedSystemButton;
    private final List<CheckBox> appChecks = new ArrayList<>();
    private final List<CheckBox> systemAppChecks = new ArrayList<>();
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
        loadOptionalSystemApps();
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

        closeAllButton = makeButton("FECHAR TODOS OS APPS DE USUÁRIO + ABRIR FCL");
        closeAllButton.setOnClickListener(v -> confirmCloseAllApps());
        root.addView(closeAllButton, buttonParams());

        TextView advancedSystemTitle = makeText("APPS DO SISTEMA — AVANÇADO", 14,
                Color.rgb(255, 190, 110));
        LinearLayout.LayoutParams advancedParams = matchWrap();
        advancedParams.topMargin = dp(24);
        advancedParams.bottomMargin = dp(4);
        root.addView(advancedSystemTitle, advancedParams);

        TextView advancedSystemHint = makeText("Lista filtrada de apps de sistema com ícone de abertura. Nenhum fica marcado automaticamente. Mesmo assim, forçar parada pode quebrar funções temporariamente; componentes essenciais são excluídos da lista.", 12, Color.LTGRAY);
        root.addView(advancedSystemHint, matchWrap());

        systemAppList = new LinearLayout(this);
        systemAppList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams systemListParams = matchWrap();
        systemListParams.topMargin = dp(8);
        root.addView(systemAppList, systemListParams);

        closeSelectedSystemButton = makeButton("TENTAR FECHAR SISTEMA SELECIONADO + ABRIR FCL");
        closeSelectedSystemButton.setOnClickListener(v -> confirmCloseSelectedSystemApps());
        root.addView(closeSelectedSystemButton, buttonParams());

        TextView systemTitle = makeText("AJUSTES DO ANDROID (REVERSÍVEIS)", 14,
                Color.rgb(151, 245, 174));
        LinearLayout.LayoutParams systemTitleParams = matchWrap();
        systemTitleParams.topMargin = dp(24);
        systemTitleParams.bottomMargin = dp(4);
        root.addView(systemTitle, systemTitleParams);

        TextView systemHint = makeText("Estas opções alteram configurações globais via Shizuku. O limite de processos pode atrasar notificações ou recarregar apps.", 12, Color.LTGRAY);
        root.addView(systemHint, matchWrap());

        animationsButton = makeButton("TURBO VISUAL: DESATIVAR ANIMAÇÕES");
        animationsButton.setOnClickListener(v -> runSystemSettingMode(true));
        root.addView(animationsButton, buttonParams());

        processLimitButton = makeButton("MODO AGRESSIVO: LIMITAR APPS EM SEGUNDO PLANO");
        processLimitButton.setOnClickListener(v -> confirmProcessLimit());
        root.addView(processLimitButton, buttonParams());

        restoreButton = makeButton("RESTAURAR CONFIGURAÇÕES PADRÃO");
        restoreButton.setOnClickListener(v -> runSystemSettingMode(false));
        root.addView(restoreButton, buttonParams());

        TextView note = makeText("O modo extremo pode interromper apps selecionados e alterar animações/limite de processos do Android via Shizuku. Use restaurar para voltar às configurações padrão. Não altera kernel, voltagem ou temperatura.", 12, Color.GRAY);
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

    private boolean isProtectedCorePackage(String pkg) {
        String[] exact = {
                "android", "com.android.systemui", "com.android.settings",
                "com.android.phone", "com.android.server.telecom", "com.android.shell",
                "com.android.permissioncontroller", "com.google.android.permissioncontroller",
                "com.android.packageinstaller", "com.android.providers.settings",
                "com.android.providers.media", "com.android.providers.downloads",
                "com.android.providers.downloads.ui", "com.android.providers.contacts",
                "com.android.providers.telephony", "com.android.inputmethod.latin",
                "com.google.android.gms", "com.google.android.gsf", "com.android.vending",
                "com.android.bluetooth", "com.android.nfc", "com.android.networkstack",
                "com.android.documentsui", "com.android.launcher3",
                "com.sec.android.app.launcher", "com.samsung.android.knox.containercore",
                "com.samsung.android.app.telephonyui", "com.samsung.android.honeyboard",
                "com.samsung.android.packageinstaller", "com.samsung.android.sm",
                "com.samsung.android.lool", "com.android.emergency"
        };
        for (String core : exact) if (core.equals(pkg)) return true;
        String[] protectedPrefixes = {
                "com.android.providers.", "com.android.server.",
                "com.android.networkstack.", "com.google.android.modulemetadata",
                "com.samsung.android.knox.", "com.sec.android.app.SecSetupWizard"
        };
        for (String prefix : protectedPrefixes) if (pkg.startsWith(prefix)) return true;
        return false;
    }

    private void loadOptionalSystemApps() {
        systemAppList.removeAllViews();
        systemAppChecks.clear();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<ApplicationInfo> candidates = new ArrayList<>();
        for (ApplicationInfo app : installed) {
            if (app.packageName.equals(getPackageName()) || app.packageName.equals(FCL_PACKAGE)) continue;
            boolean system = (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    || (app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
            if (!system || isProtectedCorePackage(app.packageName)) continue;
            if (pm.getLaunchIntentForPackage(app.packageName) == null) continue;
            candidates.add(app);
        }
        Collections.sort(candidates, new Comparator<ApplicationInfo>() {
            @Override public int compare(ApplicationInfo a, ApplicationInfo b) {
                return String.valueOf(pm.getApplicationLabel(a))
                        .compareToIgnoreCase(String.valueOf(pm.getApplicationLabel(b)));
            }
        });
        if (candidates.isEmpty()) {
            systemAppList.addView(makeText("Nenhum app de sistema opcional foi encontrado.", 13, Color.LTGRAY), matchWrap());
            return;
        }
        for (ApplicationInfo app : candidates) {
            CheckBox check = new CheckBox(this);
            check.setText(String.valueOf(pm.getApplicationLabel(app)) + "\\n" + app.packageName);
            check.setTextColor(Color.rgb(255, 220, 185));
            check.setTextSize(13);
            check.setTag(app.packageName);
            check.setPadding(dp(2), dp(4), dp(2), dp(4));
            systemAppList.addView(check, matchWrap());
            systemAppChecks.add(check);
        }
    }

    private void confirmCloseSelectedSystemApps() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("⚠️ Interromper apps de sistema?")
                .setMessage("AVISO FORTE: você selecionou apps do sistema/fabricante. Mesmo com filtros de componentes essenciais, parar um deles pode desativar temporariamente recursos do telefone, causar erros ou fazer o app reiniciar. Nenhum app está pré-selecionado. Continue apenas se aceitar o risco.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("ENTENDO, CONTINUAR", (dialog, which) -> closeSelectedSystemApps())
                .show();
    }

    private void closeSelectedSystemApps() {
        if (!hasShizukuPermission()) return;
        final Intent fclIntent = getPackageManager().getLaunchIntentForPackage(FCL_PACKAGE);
        if (fclIntent == null) {
            statusText.setText("FCL não encontrado: " + FCL_PACKAGE);
            return;
        }
        final List<String> selected = new ArrayList<>();
        for (CheckBox check : systemAppChecks) {
            if (check.isChecked() && check.getTag() instanceof String) {
                String pkg = (String) check.getTag();
                if (pkg.matches("[A-Za-z0-9_.]+") && !pkg.equals(FCL_PACKAGE)
                        && !pkg.equals(getPackageName()) && !isProtectedCorePackage(pkg)) {
                    selected.add(pkg);
                }
            }
        }
        if (selected.isEmpty()) {
            statusText.setText("Nenhum app de sistema foi selecionado.");
            return;
        }
        statusText.setText("Tentando interromper " + selected.size() + " app(s) de sistema…");
        setButtonsEnabled(false);
        worker.execute(() -> {
            int closed = 0;
            List<String> errors = new ArrayList<>();
            for (String pkg : selected) {
                Process process = null;
                try {
                    process = Shizuku.newProcess(new String[]{"sh", "-c",
                            "am force-stop --user current " + pkg + " 2>&1"}, null, null);
                    String output = readProcessOutput(process.getInputStream());
                    int exit = process.waitFor();
                    if (exit == 0) closed++;
                    else errors.add(pkg + (output.isEmpty() ? "" : ": " + output.trim()));
                    process.destroy();
                } catch (Exception e) {
                    errors.add(pkg + ": " + e.getMessage());
                    if (process != null) process.destroy();
                }
            }
            final int count = closed;
            final int total = selected.size();
            final String errorText = errors.isEmpty() ? "" : "\\nFalhas: "
                    + String.join("; ", errors.subList(0, Math.min(3, errors.size())));
            runOnUiThread(() -> {
                setButtonsEnabled(true);
                refreshMemory();
                statusText.setText("Apps de sistema interrompidos: " + count + "/" + total
                        + ". Abrindo FCL…" + errorText);
                try {
                    fclIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(fclIntent);
                } catch (Exception e) {
                    statusText.setText("Interrupção concluída: " + count + ". Falha ao abrir FCL: " + e.getMessage());
                }
            });
        });
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

    private void confirmCloseAllApps() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("⚠️ Fechar todos os aplicativos?")
                .setMessage("AVISO: isso tentará forçar o fechamento de TODOS os aplicativos de usuário que o Android listar, exceto o RAM Extreme e o FCL. Você pode perder dados não salvos; música, downloads, alarmes de apps, sincronização e notificações podem parar. Alguns apps protegidos pelo Android ou pelo fabricante podem não fechar. O sistema e os serviços essenciais serão preservados. Deseja continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("SIM, FECHAR TODOS", (dialog, which) -> closeAllUserApps())
                .show();
    }

    private void closeAllUserApps() {
        if (!hasShizukuPermission()) return;
        final Intent fclIntent = getPackageManager().getLaunchIntentForPackage(FCL_PACKAGE);
        if (fclIntent == null) {
            statusText.setText("FCL não encontrado: " + FCL_PACKAGE);
            return;
        }

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> packages = new ArrayList<>();
        for (ApplicationInfo app : installed) {
            if (app.packageName.equals(getPackageName()) || app.packageName.equals(FCL_PACKAGE)) continue;
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            if (pm.getLaunchIntentForPackage(app.packageName) == null) continue;
            if (app.packageName.matches("[A-Za-z0-9_.]+")) packages.add(app.packageName);
        }

        statusText.setText("Fechando " + packages.size() + " apps de usuário. Aguarde…");
        setButtonsEnabled(false);
        worker.execute(() -> {
            int closed = 0;
            List<String> errors = new ArrayList<>();
            for (String pkg : packages) {
                Process process = null;
                try {
                    process = Shizuku.newProcess(
                            new String[]{"sh", "-c", "am force-stop --user current " + pkg + " 2>&1"},
                            null, null);
                    String output = readProcessOutput(process.getInputStream());
                    int exit = process.waitFor();
                    if (exit == 0) closed++;
                    else errors.add(pkg + (output.isEmpty() ? "" : ": " + output.trim()));
                    process.destroy();
                } catch (Exception e) {
                    errors.add(pkg + ": " + e.getMessage());
                    if (process != null) process.destroy();
                }
            }

            final int closedCount = closed;
            final int total = packages.size();
            final String errorText = errors.isEmpty() ? "" : "\nAlgumas falhas: "
                    + String.join("; ", errors.subList(0, Math.min(3, errors.size())));
            runOnUiThread(() -> {
                setButtonsEnabled(true);
                refreshMemory();
                statusText.setText("Fechamento em massa concluído: " + closedCount + "/" + total
                        + " apps interrompidos." + errorText + "\nAbrindo FCL…");
                try {
                    fclIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(fclIntent);
                } catch (Exception e) {
                    statusText.setText("Apps interrompidos: " + closedCount + ". Falha ao abrir FCL: " + e.getMessage());
                }
            });
        });
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

    private void confirmProcessLimit() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Ativar limite agressivo?")
                .setMessage("O Android será configurado para manter no máximo 2 processos em segundo plano. Isso pode interromper música, downloads, notificações e outros apps. Você pode restaurar o padrão aqui.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Ativar", (dialog, which) -> runProcessLimit())
                .show();
    }

    private boolean hasShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            updateShizukuStatus();
            statusText.setText("Inicie o Shizuku pela Depuração sem fio e tente novamente.");
            return false;
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            statusText.setText("Autorize o RAM Extreme no Shizuku antes de alterar configurações.");
            requestShizukuPermission();
            return false;
        }
        return true;
    }

    private void runSystemSettingMode(boolean extreme) {
        if (!hasShizukuPermission()) return;
        String[][] commands = extreme
                ? new String[][]{
                    {"settings", "put", "global", "window_animation_scale", "0"},
                    {"settings", "put", "global", "transition_animation_scale", "0"},
                    {"settings", "put", "global", "animator_duration_scale", "0"}
                }
                : new String[][]{
                    {"settings", "put", "global", "window_animation_scale", "1"},
                    {"settings", "put", "global", "transition_animation_scale", "1"},
                    {"settings", "put", "global", "animator_duration_scale", "1"},
                    {"settings", "put", "global", "background_process_limit", "0"}
                };
        runCommands(commands, extreme
                ? "Desativando animações do Android…"
                : "Restaurando animações e limite padrão…",
                extreme
                ? "Animações desativadas. Isso deixa transições mais rápidas, mas não aumenta diretamente o FPS do Minecraft."
                : "Animações restauradas para 1x e limite de processos em segundo plano restaurado para padrão (0).");
    }

    private void runProcessLimit() {
        if (!hasShizukuPermission()) return;
        runCommands(new String[][]{
                {"settings", "put", "global", "background_process_limit", "2"}
        }, "Aplicando limite de processos em segundo plano…",
                "Limite configurado para 2 processos em segundo plano. Se algum app parar de funcionar corretamente, toque em RESTAURAR CONFIGURAÇÕES PADRÃO.");
    }

    private void runCommands(String[][] commands, String progress, String successMessage) {
        statusText.setText(progress);
        setButtonsEnabled(false);
        worker.execute(() -> {
            List<String> errors = new ArrayList<>();
            int success = 0;
            for (String[] command : commands) {
                Process process = null;
                try {
                    process = Shizuku.newProcess(new String[]{"sh", "-c", String.join(" ", command) + " 2>&1"}, null, null);
                    String output = readProcessOutput(process.getInputStream());
                    int exit = process.waitFor();
                    if (exit == 0) {
                        success++;
                    } else {
                        errors.add(String.join(" ", command) + (output.isEmpty() ? "" : ": " + output.trim()));
                    }
                    process.destroy();
                } catch (Exception e) {
                    errors.add(String.join(" ", command) + ": " + e.getMessage());
                    if (process != null) process.destroy();
                }
            }
            final int applied = success;
            final int total = commands.length;
            final String errorText = errors.isEmpty() ? "" : "\nFalhas: "
                    + String.join("; ", errors.subList(0, Math.min(3, errors.size())));
            runOnUiThread(() -> {
                setButtonsEnabled(true);
                statusText.setText(applied + "/" + total + " comandos aplicados. "
                        + (errors.isEmpty() ? successMessage : "Confira as falhas reportadas.") + errorText);
                refreshMemory();
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
        if (animationsButton != null) animationsButton.setEnabled(enabled);
        if (processLimitButton != null) processLimitButton.setEnabled(enabled);
        if (restoreButton != null) restoreButton.setEnabled(enabled);
        if (closeAllButton != null) closeAllButton.setEnabled(enabled);
        if (closeSelectedSystemButton != null) closeSelectedSystemButton.setEnabled(enabled);
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
