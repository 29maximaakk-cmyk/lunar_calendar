
```python
# lunar_calendar.py
import argparse
import json
import csv
import math
import sys
from datetime import datetime, timedelta
from colorama import init, Fore, Style

init(autoreset=True)

class LunarCalendar:
    # Знаки зодиака (тропические)
    ZODIAC_SIGNS = ["Овен", "Телец", "Близнецы", "Рак", "Лев", "Дева",
                    "Весы", "Скорпион", "Стрелец", "Козерог", "Водолей", "Рыбы"]
    # Рекомендации для каждой фазы (общие)
    RECOMMENDATIONS = {
        "новолуние": {"посадка": "неблагоприятно", "полив": "допустимо", "обрезка": "неблагоприятно", "урожай": "неблагоприятно", "вредители": "благоприятно"},
        "первая четверть": {"посадка": "благоприятно", "полив": "благоприятно", "обрезка": "неблагоприятно", "урожай": "неблагоприятно", "вредители": "неблагоприятно"},
        "полнолуние": {"посадка": "неблагоприятно", "полив": "допустимо", "обрезка": "благоприятно", "урожай": "благоприятно", "вредители": "благоприятно"},
        "последняя четверть": {"посадка": "неблагоприятно", "полив": "неблагоприятно", "обрезка": "благоприятно", "урожай": "благоприятно", "вредители": "благоприятно"}
    }

    def __init__(self, date_str=None):
        if date_str:
            self.date = datetime.strptime(date_str, "%Y-%m-%d")
        else:
            self.date = datetime.now()
        self.lunar_day = None
        self.phase = None
        self.zodiac_sign = None
        self.calculate()

    def calculate(self):
        # Алгоритм расчёта возраста Луны (по Жану Меёсу, упрощённый)
        # Вычисляем количество дней с известного новолуния (2000-01-06 18:14 UTC)
        base = datetime(2000, 1, 6, 18, 14)
        delta = (self.date - base).total_seconds() / 86400
        # Средний синодический месяц = 29.53058867 дней
        lunar_age = delta % 29.53058867
        self.lunar_day = int(lunar_age) + 1  # день от 1 до 30
        if self.lunar_day > 30:
            self.lunar_day = 30
        # Определение фазы
        if self.lunar_day <= 1 or self.lunar_day > 29:
            self.phase = "новолуние"
        elif 1 < self.lunar_day <= 7.4:
            self.phase = "первая четверть"
        elif 7.4 < self.lunar_day <= 14.8:
            self.phase = "полнолуние"
        elif 14.8 < self.lunar_day <= 22.2:
            self.phase = "последняя четверть"
        else:
            self.phase = "новолуние"  # остаток
        # Знак зодиака Луны (приблизительно по долготе, вычисленной по формуле)
        # Используем эфемеридный метод: вычисляем среднюю долготу Луны
        # Для упрощения используем приближение, основанное на дате
        # Более точный расчёт требует сложных формул, здесь используем эмпирическую зависимость
        # от дня в году: каждый день Луна смещается примерно на 12.2° (среднее)
        # и имеет начальное положение 1 января 2000 года около 180°
        day_of_year = self.date.timetuple().tm_yday
        # Приблизительная долгота Луны в градусах (0-360)
        # Начальная долгота 1 января 2000 = ~180°
        years = self.date.year - 2000
        # Учёт лет (примерно 12.368 синодических месяцев в году)
        total_days = (self.date - datetime(2000, 1, 1)).total_seconds() / 86400
        # Луна движется по эклиптике ~13.176° в сутки
        longitude = (180 + total_days * 13.176) % 360
        # Определяем знак зодиака (каждый знак по 30°)
        sign_index = int(longitude // 30) % 12
        self.zodiac_sign = self.ZODIAC_SIGNS[sign_index]

    def get_recommendations(self):
        # Возвращает словарь с рекомендациями для данной фазы
        return self.RECOMMENDATIONS.get(self.phase, {})

    def to_dict(self):
        return {
            "date": self.date.strftime("%Y-%m-%d"),
            "lunar_day": self.lunar_day,
            "phase": self.phase,
            "zodiac_sign": self.zodiac_sign,
            "recommendations": self.get_recommendations()
        }

    def print(self, verbose=False, color=True):
        if color:
            print(Fore.CYAN + f"🌙 Лунный календарь на {self.date.strftime('%Y-%m-%d')}")
            print(Fore.YELLOW + f"Лунный день: {self.lunar_day}")
            print(Fore.MAGENTA + f"Фаза: {self.phase}")
            print(Fore.GREEN + f"Знак зодиака Луны: {self.zodiac_sign}")
            if verbose:
                print(Fore.WHITE + "Рекомендации:")
                rec = self.get_recommendations()
                for key, val in rec.items():
                    if val == "благоприятно":
                        color_val = Fore.GREEN
                    elif val == "неблагоприятно":
                        color_val = Fore.RED
                    else:
                        color_val = Fore.YELLOW
                    print(f"  - {key.capitalize()}: {color_val}{val}")
        else:
            print(f"Лунный календарь на {self.date.strftime('%Y-%m-%d')}")
            print(f"Лунный день: {self.lunar_day}")
            print(f"Фаза: {self.phase}")
            print(f"Знак зодиака Луны: {self.zodiac_sign}")
            if verbose:
                print("Рекомендации:")
                for key, val in self.get_recommendations().items():
                    print(f"  - {key.capitalize()}: {val}")

def main():
    parser = argparse.ArgumentParser(description="Лунный календарь садовода")
    parser.add_argument("--date", help="Дата в формате YYYY-MM-DD")
    parser.add_argument("--json", help="Экспорт в JSON")
    parser.add_argument("--csv", help="Экспорт в CSV")
    parser.add_argument("--verbose", action="store_true", help="Подробный вывод")
    parser.add_argument("--no-color", action="store_true", help="Отключить цвет")
    args = parser.parse_args()

    calendar = LunarCalendar(args.date)
    if args.no_color:
        color = False
    else:
        color = sys.stdout.isatty()
    calendar.print(verbose=args.verbose, color=color)

    if args.json:
        with open(args.json, 'w') as f:
            json.dump(calendar.to_dict(), f, indent=2, ensure_ascii=False)
        print(f"Результат сохранён в {args.json}")

    if args.csv:
        with open(args.csv, 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(["date", "lunar_day", "phase", "zodiac_sign", "recommendations"])
            rec_str = "; ".join([f"{k}:{v}" for k, v in calendar.get_recommendations().items()])
            writer.writerow([calendar.date.strftime("%Y-%m-%d"), calendar.lunar_day,
                            calendar.phase, calendar.zodiac_sign, rec_str])
        print(f"Результат сохранён в {args.csv}")

if __name__ == "__main__":
    main()
