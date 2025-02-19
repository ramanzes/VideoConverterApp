# VideoConverterApp

Простое Java-приложение с графическим интерфейсом для конвертации `.avi` файлов в `.mp4` с использованием `ffmpeg`.


## Описание

Приложение позволяет пользователю:
1. Выбрать директорию с `.avi` файлами.
2. Просмотреть список файлов в выбранной директории.
3. Запустить конвертацию всех файлов с отображением прогресса.
4. Одновременно конвертировать до 5 файлов.

## Использование

1. Убедитесь, что `ffmpeg` установлен и доступен через командную строку.
2. также должны быть устнавлены в системе `jdk-17` и `maven` и также должны быть доступны через консоль.
3. Соберите проект с помощью Maven:
   
```bash
   mvn clean package
```

1. **Прогресс-бар**:
    -  добавлен в отдельную панель (`bottomPanel`).

2. **Одновременная конвертация**:
    - Используется `ExecutorService` с пулом из 5 потоков.
    - Каждый файл конвертируется в отдельном потоке.
    - После завершения конвертации обновляется прогресс.

3. **Прогресс для нескольких файлов**:
    - Используется `AtomicInteger` для подсчета завершенных файлов.
    - Прогресс рассчитывается как процент завершенных файлов от общего количества.

---

### Как это работает:
1. Пользователь выбирает директорию.
2. В `JList` отображаются все `.avi` файлы.
3. При нажатии на кнопку "Convert" запускается конвертация до 5 файлов одновременно.
4. Прогресс отображается в `JProgressBar`.
5. По завершении конвертации каждого файла прогресс обновляется.

---

### Сборка и запуск
1. Соберите проект с помощью Maven:
   ```bash
   mvn clean package
   ```
2. Запустите приложение:
   ```bash
   java -jar target/video-converter-1.0-SNAPSHOT.jar
   ```




## Методы

### `VideoConverterApp()`
Инициализирует графический интерфейс и настраивает основные компоненты.

### `chooseDirectory()`
Открывает диалоговое окно для выбора директории и обновляет список файлов.

### `updateFileList(File directory)`
Обновляет список файлов в `JList` для отображения всех `.avi` файлов в выбранной директории.

### `startConversion()`
Запускает процесс конвертации всех `.avi` файлов в выбранной директории.

### `convertFile(File aviFile)`
Конвертирует один `.avi` файл в `.mp4` с использованием `ffmpeg`.

### `main(String[] args)`
Точка входа в программу. Запускает графический интерфейс.

## Зависимости
- Java 17 или выше.
- `ffmpeg` (должен быть установлен в системе).

## Лицензия
MIT




### Описание методов программы

#### 1. **`VideoConverterApp()`**
- **Описание**: Конструктор класса `VideoConverterApp`. Инициализирует графический интерфейс и настраивает основные компоненты.
- **Детали**:
    - Создает окно приложения с заголовком "Video Converter".
    - Настраивает текстовое поле (`directoryField`) для отображения выбранной директории.
    - Добавляет кнопку "Browse" для выбора директории.
    - Создает список (`JList`) для отображения `.avi` файлов в выбранной директории.
    - Добавляет прогресс-бар (`JProgressBar`) для отображения прогресса конвертации.
    - Добавляет текстовую область (`JTextArea`) для вывода логов.
    - Настраивает кнопку "Convert" для запуска конвертации.
    - Инициализирует пул потоков (`ExecutorService`) для одновременной конвертации до 5 файлов.

#### 2. **`chooseDirectory()`**
- **Описание**: Открывает диалоговое окно для выбора директории и обновляет список файлов.
- **Детали**:
    - Использует `JFileChooser` для выбора директории.
    - Если пользователь выбирает директорию, путь к ней отображается в текстовом поле (`directoryField`).
    - Вызывает метод `updateFileList()` для обновления списка файлов.

#### 3. **`updateFileList(File directory)`**
- **Описание**: Обновляет список файлов в `JList` для отображения всех `.avi` файлов в выбранной директории.
- **Детали**:
    - Очищает текущий список файлов.
    - Получает все файлы с расширением `.avi` в выбранной директории.
    - Добавляет имена файлов в `DefaultListModel`, который связан с `JList`.

#### 4. **`startConversion()`**
- **Описание**: Запускает процесс конвертации всех `.avi` файлов в выбранной директории.
- **Детали**:
    - Проверяет, выбрана ли директория. Если нет, выводит сообщение об ошибке.
    - Получает все `.avi` файлы в выбранной директории.
    - Сбрасывает прогресс-бар и счетчик завершенных файлов.
    - Для каждого файла создает задачу в пуле потоков (`ExecutorService`), которая вызывает метод `convertFile()`.
    - После завершения конвертации каждого файла обновляет прогресс-бар.

#### 5. **`convertFile(File aviFile)`**
- **Описание**: Конвертирует один `.avi` файл в `.mp4` с использованием `ffmpeg`.
- **Детали**:
    - Создает команду для запуска `ffmpeg` с параметрами конвертации.
    - Запускает процесс конвертации с помощью `ProcessBuilder`.
    - Ожидает завершения процесса и проверяет код завершения.
    - Если конвертация успешна, добавляет запись в лог. В случае ошибки также добавляет соответствующее сообщение.
    - Обновляет прогресс-бар через `SwingUtilities.invokeLater()` для корректного отображения в GUI.

#### 6. **`main(String[] args)`**
- **Описание**: Точка входа в программу. Запускает графический интерфейс.
- **Детали**:
    - Использует `SwingUtilities.invokeLater()` для запуска GUI в потоке обработки событий (Event Dispatch Thread).
    - Создает экземпляр класса `VideoConverterApp` и делает его видимым.



