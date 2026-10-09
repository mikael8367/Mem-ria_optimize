# RAM Extreme

App Android para Galaxy A12 / Android 12, com Shizuku sem root, focado em preparar o aparelho antes de abrir FCL (`com.tungsten.fcl`).

## Funcionalidades atuais
- Mostra RAM total, uso aproximado e disponível.
- Verifica se o serviço Shizuku está ativo e solicita autorização.
- Lista apps de usuário com atividade de inicialização, excluindo apps do sistema, o próprio RAM Extreme e o FCL.
- Permite selecionar explicitamente os apps a interromper.
- Ao ativar o modo extremo, executa `am force-stop --user current <pacote>` via Shizuku para cada app selecionado e depois tenta abrir `com.tungsten.fcl`.
- Informa quantos comandos terminaram com código de sucesso e mostra até três falhas.

## Atenção
Force-stop pode encerrar tarefas não salvas e impede o app selecionado de continuar suas atividades até ser iniciado novamente. Marque apenas apps que aceita interromper. Não selecionamos apps automaticamente. Isso não garante aumento de FPS nem uma quantidade fixa de RAM liberada; o Android continua gerenciando a memória.

## Instalação e uso
1. Inicie Shizuku por Depuração sem fio no Android 11+.
2. Abra RAM Extreme e toque em **VERIFICAR / AUTORIZAR SHIZUKU**.
3. Conceda autorização.
4. Marque os apps que deseja interromper.
5. Toque em **ATIVAR MODO EXTREMO + ABRIR FCL**.

O Shizuku iniciado via Depuração sem fio normalmente precisa ser iniciado de novo após reiniciar o telefone.

## APK
Cada push para `main` aciona GitHub Actions. Abra a aba **Actions**, selecione **Build RAM Extreme APK** e baixe o artefato `RAM-Extreme-debug` quando a execução terminar com sucesso.
