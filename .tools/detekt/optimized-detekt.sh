#!/bin/bash

# Скрипт должен быть запущен из репозитория с android проектом

start_time=$(date +%s)  # Записываем время начала выполнения скрипта

function generateDetektBaseline() {
  echo "Generating Detekt baseline..."
  ./.tools/detekt/generate-detekt-baseline.sh -eq 0 || exit 1
  echo "Baseline was generated"
}

# Определяем рабочую директорию проекта
project_dir=$(git rev-parse --show-toplevel)
echo "project_dir="$project_dir
cd "$project_dir"

# Получаем список измененных файлов из git и фильтруем только .kt файлы
commited_changes=$(git diff --relative --name-only origin/develop... | grep '\.kt\?$')
uncommited_changes=$(git diff --relative --name-only HEAD | grep '\.kt\?$')

if [ -z "$commited_changes" ]; then
  staged_changes="$uncommited_changes"
elif [ -z "$uncommited_changes" ]; then
  staged_changes="$commited_changes"
else
  staged_changes="$commited_changes,$uncommited_changes"
fi

# Получаем список изменённых или удалённых detekt-baseline.xml файлов
commited_baseline_changes=$(git diff --relative --name-only origin/develop... | grep detekt-baseline.xml)
uncommited_baseline_changes=$(git diff --relative --name-only HEAD | grep detekt-baseline.xml)
if [ -z "$commited_baseline_changes" ]; then
  baseline_changes="$uncommited_baseline_changes"
elif [ -z "$uncommited_baseline_changes" ]; then
  baseline_changes="$commited_baseline_changes"
else
  baseline_changes="$commited_baseline_changes,$uncommited_baseline_changes"
fi

# Добавляем в список изменений все kt файлы из модулей, где изменились baseline
baseline_changes_list=$(echo "$baseline_changes" | grep -v '^$' | paste -sd, -)
IFS=',' read -ra ADDR <<< "$baseline_changes_list"
for file in "${ADDR[@]}"; do
  module_folder=$(dirname "$file")
  echo "Baseline changed, adding all kt files from $module_folder to changes list"
  module_files=$(find "$module_folder" -type f -name "*.kt" ! -path "$module_folder/build/*")
  if [ -z "$staged_changes" ]; then
    staged_changes="$module_files"
  else
    staged_changes="$staged_changes,$module_files"
  fi
done

changes_list=$(echo "$staged_changes" | grep -v '^$' | paste -sd, -)

# Проверяем, что каждый файл в списке реально существует
IFS=',' read -ra ADDR <<< "$changes_list"
changes_list=""
for file in "${ADDR[@]}"; do
  if [ -f "$file" ]; then
    if [ -z "$changes_list" ]; then
      changes_list="$file"
    else
      changes_list="$changes_list,$file"
    fi
  fi
done

echo "changes_list="$changes_list

# Проверяем, что список изменений не пустой
if [ -z "$changes_list" ]; then
  echo "No files to process."
  exit 0
fi

# Вызываем скрипт для генерации baseline файла для detekt
generateDetektBaseline

# Получаем список плагинов для detekt, включая собственные кастомные правила
plugins=$(find "$project_dir"/build_scripts/detekt/rulesets -type f -name "*.jar" | tr '\n' ',' | sed 's/,$//')

# Создаем папку и файл для конфигурации запуска. Файл нужен, чтобы не ловить ошибку запуска скрипта при большом
# количестве изменений в проекте (`Argument list too long`)
config_dir_path="$project_dir/build/tmp"
mkdir -p "$config_dir_path"
config_file_path="$config_dir_path/detekt_cli_args"

# Записываем конфигурацию запуска Detekt (пример: https://github.com/detekt/detekt/pull/2397/files)
echo "--build-upon-default-config" > "$config_file_path"
echo "--config" >> "$config_file_path"
echo "$project_dir/build_scripts/detekt/detekt-config.yml" >> "$config_file_path"
echo "--input" >> "$config_file_path"
echo "$changes_list" >> "$config_file_path"
echo "--plugins" >> "$config_file_path"
echo "$plugins" >> "$config_file_path"
echo "--report" >> "$config_file_path"
echo "html:detekt_report.html" >> "$config_file_path"
echo "--parallel" >> "$config_file_path"
echo "--baseline" >> "$config_file_path"
echo "$project_dir/build/reports/detekt/cli-baseline.xml" >> "$config_file_path"

# Запускаем detekt на измененных файлах
output=$("$project_dir"/build_scripts/detekt/detekt-cli/cli/detekt-cli @"$config_file_path")

if [ -n "$output" ]; then
  report_path="file://$project_dir/detekt_report.html"
  echo "report_path="$report_path
  echo "$output" | sed 'G'
  echo "Detailed report available at $report_path"

  exit 1
else
  echo "No detekt violations indicated"
fi

end_time=$(date +%s)  # Записываем время окончания выполнения скрипта
elapsed_time=$((end_time - start_time))  # Вычисляем разницу во времени
echo "Optimized detekt duration: $elapsed_time seconds"  # Выводим время выполнения скрипта