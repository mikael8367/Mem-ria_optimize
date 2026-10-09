# RAM Extreme

App Android para Galaxy A12 / Android 12, com Shizuku sem root, focado em preparar o aparelho antes de abrir FCL (`com.tungsten.fcl`).

## Funcionalidades
- Interface reorganizada em quatro abas: Início, Apps, Sistema e Ajustes.
- Capa gráfica local em verde/preto, sem depender de conexão com a internet.
- Salva automaticamente os aplicativos selecionados e a última aba usada ao sair/retomar o app.
- Mostra RAM total, uso aproximado e disponível.
- Verifica Shizuku e solicita autorização.
- Lista apps comuns do usuário, excluindo apps do sistema, o próprio RAM Extreme e o FCL.
- Permite selecionar apps para interromper via `am force-stop` antes de abrir o FCL.
- Inclui botão separado para tentar interromper todos os apps de usuário de uma vez, com aviso e confirmação. Exclui o próprio RAM Extreme, o FCL e apps marcados como sistema; alguns apps protegidos podem não fechar.
- Lista separada de apps de sistema/fabricante que têm tela de abertura, sem seleção automática. O usuário precisa marcar cada app e confirmar um aviso forte; componentes Android considerados essenciais são filtrados.
- Desativa as três escalas de animação do Android (0x) por comandos `settings put global`.
- Oferece um limite agressivo de dois processos em segundo plano.
- Restaura as animações para 1x e o limite de processos para o padrão (0).

## Atenção
A opção **FECHAR TODOS OS APPS DE USUÁRIO + ABRIR FCL** é agressiva. A opção **TENTAR FECHAR SISTEMA SELECIONADO + ABRIR FCL** tenta interromper apenas apps de sistema que você marcou, após um segundo aviso; os filtros de componentes essenciais não são infalíveis: tenta fechar todos os apps comuns de usuário, mas não serviços essenciais nem apps identificados como sistema. Alguns apps do fabricante podem não fechar. Pode descartar trabalho não salvo. O limite de dois processos pode atrasar notificações, parar áudio/downloads e fazer apps recarregarem. Use **RESTAURAR CONFIGURAÇÕES PADRÃO** para restaurar animações e limite padrão. Se quiser apenas restaurar o limite, essa opção também o faz. Estas alterações são reversíveis e exigem Shizuku autorizado.

Desativar animações melhora a rapidez visual, não aumenta diretamente os FPS do Minecraft. Limitar processos pode liberar alguma memória em certos casos, mas também pode piorar a experiência. O Android continua gerenciando a RAM; não existe garantia de ganho fixo de RAM/FPS. O app não altera kernel, voltagem, frequência de CPU/GPU ou limites térmicos.

## Uso
1. Inicie Shizuku pela Depuração sem fio no Android 11+.
2. Abra RAM Extreme e autorize o app.
3. Selecione manualmente os apps comuns que aceita interromper.
4. Use **ATIVAR MODO EXTREMO + ABRIR FCL** para interrompê-los e abrir `com.tungsten.fcl`.
5. Para ajustes do sistema, use os botões de animações ou limite agressivo. Para reverter, toque em **RESTAURAR CONFIGURAÇÕES PADRÃO**.

Shizuku iniciado pela Depuração sem fio normalmente precisa ser iniciado de novo após reiniciar o telefone.

## APK
Cada push para `main` aciona GitHub Actions. Abra **Actions**, selecione **Build RAM Extreme APK** e baixe o artefato `RAM-Extreme-debug` quando a execução terminar com sucesso.
