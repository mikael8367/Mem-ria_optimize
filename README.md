# RAM Extreme

Aplicativo Android inicial para Galaxy A12 (Android 12), com integração ao Shizuku sem root e foco no launcher FCL.

## O que esta versão faz
- Mostra a memória total, em uso aproximado e disponível.
- Verifica se o Shizuku está ativo e solicita autorização.
- Tenta localizar um app instalado cujo nome contenha FCL ou Fold Craft e abri-lo.
- Não encerra processos automaticamente: isso pode piorar o desempenho e reiniciar apps.

## Gerar APK
Cada push para `main` inicia o GitHub Actions. Abra **Actions**, selecione **Build RAM Extreme APK** e baixe o artefato **RAM-Extreme-debug**.

## Usar
1. Instale e inicie Shizuku usando Depuração sem fio no Android 11+.
2. Abra RAM Extreme e toque em **VERIFICAR / AUTORIZAR SHIZUKU**.
3. Conceda a autorização e toque em **PREPARAR E ABRIR FCL**.

O Shizuku iniciado via depuração sem fio normalmente precisa ser iniciado novamente após reiniciar o telefone.

## Limites da versão 0.1.0
Esta é uma base inicial: mede RAM, verifica Shizuku e tenta abrir o FCL. Ainda não aplica ajustes privilegiados de desempenho. Não é possível prometer liberar toda a RAM nem garantir aumento de FPS; as próximas alterações precisam ser testadas no aparelho real.
