## Impact-Analysis-Tool

Данный sample предназначен, чтобы продемонстрировать возмжности инструмента анализа зависимостей
между модулями.
Инструмент позволяет выборочно запускать проверки на PR (напр. detekt или unit тесты) только для тех
модулей, которые были затронуты изменениями в PR.

### Demo

Вы можете создать новый файл под git-index через Android Studio или выполнить команды
```
echo "Hello Impact" >> ./modules/common/base-database/NewFile.kt
git add --all
```

После чего запустить выполнение тестов и detekt при помощи
```
./.tools/optimized-checks -d -t
```

Таск должен завершиться с ошибкой при выполнении теста

```
Feature1ImplTest > broken test FAILED
    java.lang.AssertionError at Feature1ImplTest.kt:11
```

Дальше смотрим логи и видим, что

1. В changes list detekt попал только один файл

```
changes_list=modules/common/base-database/NewFile.kt
Generating Detekt baseline...
Generation baseline duration: 0 seconds
Baseline was generated
No detekt violations indicated
Optimized detekt duration: 1 seconds
```

2. Для тестов impact-анализ определил как изменившиеся 4 файла

```
Impact Analysis.
Changed 1 modules: [base-database].
Affected 1 modules: [core].
Transitively Affected 2 modules: [feature1_api, feature1_impl]
```

Что соответствует графу модулей ниже

### Граф модулей

```mermaid
%%{
  init: {
    'theme': 'base',
    'themeVariables': {"primaryTextColor":"#fff","primaryColor":"#5a4f7c","primaryBorderColor":"#5a4f7c","lineColor":"#f5a623","tertiaryColor":"#40375c","fontSize":"12px"}
  }
}%%

graph LR
  :feature1_impl --> :core
  :feature1_impl --> :ui-kit
  :feature1_impl --> :feature1_api
  :feature2_impl --> :ui-kit
  :feature2_impl --> :feature2_api
  :feature2_api --> :ui-kit
  :core --> :base-network
  :core --> :base-device
  :core --> :base-database
  :feature1_api --> :core
  :feature1_api --> :ui-kit
```

Заводомо сломанный тест, как раз находится в модуле feature1_impl. Таким образом вы можете
поэкспериментировать с изменениями в разных модулях и убедиться, что инструмент анализа зависимостей
работает корректно.
