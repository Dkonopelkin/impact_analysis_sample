#!/bin/bash

# Записываем время начала выполнения скрипта
start_time=$(date +%s)

# Находим все файлы с именем detekt-baseline.xml в подкаталогах текущего каталога
detekt_baseline_list=$(find . -name 'detekt-baseline.xml')

# Определяем пути и создаем необходимые директории
output_dir="./build/reports/detekt"
output_file="$output_dir/cli-baseline.xml"
mkdir -p "$output_dir"

# Функция для добавления содержимого тегов <ID>
add_issues() {
    local tag_name="$1"
    local output="$2"
    echo "  <$tag_name>" >> "$output"
    for file in $detekt_baseline_list; do
        awk "/<$tag_name>/,/<\/$tag_name>/{print}" "$file" | grep '<ID>' >> "$output"
    done
    echo "  </$tag_name>" >> "$output"
}

# Начинаем формирование результирующего файла
{
    echo "<?xml version='1.0' encoding='UTF-8'?>"
    echo "<SmellBaseline>"
} > "$output_file"

# Добавляем теги ManuallySuppressedIssues и CurrentIssues
add_issues "ManuallySuppressedIssues" "$output_file"
add_issues "CurrentIssues" "$output_file"

# Завершаем формирование файла
echo "</SmellBaseline>" >> "$output_file"

# Записываем время окончания выполнения скрипта
end_time=$(date +%s)

# Вычисляем и выводим затраченное время
elapsed_time=$((end_time - start_time))
echo "Generation baseline duration: $elapsed_time seconds"

# Завершаем скрипт с кодом выхода 0
exit 0