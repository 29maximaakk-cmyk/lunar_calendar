// LunarCalendar.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading.Tasks;

namespace LunarCalendar
{
    class Program
    {
        static readonly string[] ZodiacSigns = {"Овен","Телец","Близнецы","Рак","Лев","Дева",
            "Весы","Скорпион","Стрелец","Козерог","Водолей","Рыбы"};

        static readonly Dictionary<string, Dictionary<string, string>> Recommendations = new()
        {
            ["новолуние"] = new() { {"посадка","неблагоприятно"}, {"полив","допустимо"}, {"обрезка","неблагоприятно"}, {"урожай","неблагоприятно"}, {"вредители","благоприятно"} },
            ["первая четверть"] = new() { {"посадка","благоприятно"}, {"полив","благоприятно"}, {"обрезка","неблагоприятно"}, {"урожай","неблагоприятно"}, {"вредители","неблагоприятно"} },
            ["полнолуние"] = new() { {"посадка","неблагоприятно"}, {"полив","допустимо"}, {"обрезка","благоприятно"}, {"урожай","благоприятно"}, {"вредители","благоприятно"} },
            ["последняя четверть"] = new() { {"посадка","неблагоприятно"}, {"полив","неблагоприятно"}, {"обрезка","благоприятно"}, {"урожай","благоприятно"}, {"вредители","благоприятно"} }
        };

        class LunarData
        {
            public string Date { get; set; }
            public int LunarDay { get; set; }
            public string Phase { get; set; }
            public string ZodiacSign { get; set; }
            public Dictionary<string, string> Recommendations { get; set; }
        }

        static LunarData Calculate(DateTime date)
        {
            // Базовое новолуние: 2000-01-06 18:14 UTC
            DateTime baseDate = new DateTime(2000, 1, 6, 18, 14, 0, DateTimeKind.Utc);
            double diff = (date.ToUniversalTime() - baseDate).TotalDays;
            double lunarAge = diff % 29.53058867;
            int lunarDay = (int)Math.Floor(lunarAge) + 1;
            if (lunarDay > 30) lunarDay = 30;
            string phase;
            if (lunarDay <= 1 || lunarDay > 29) phase = "новолуние";
            else if (lunarDay <= 7) phase = "первая четверть";
            else if (lunarDay <= 14) phase = "полнолуние";
            else if (lunarDay <= 22) phase = "последняя четверть";
            else phase = "новолуние";
            // Знак зодиака
            DateTime start = new DateTime(2000, 1, 1, 0, 0, 0, DateTimeKind.Utc);
            double totalDays = (date.ToUniversalTime() - start).TotalDays;
            double longitude = (180 + totalDays * 13.176) % 360;
            int signIndex = (int)(longitude / 30) % 12;
            string sign = ZodiacSigns[signIndex];

            return new LunarData
            {
                Date = date.ToString("yyyy-MM-dd"),
                LunarDay = lunarDay,
                Phase = phase,
                ZodiacSign = sign,
                Recommendations = Recommendations.ContainsKey(phase) ? Recommendations[phase] : new Dictionary<string, string>()
            };
        }

        static void PrintData(LunarData data, bool verbose, bool color)
        {
            if (color)
            {
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine($"🌙 Лунный календарь на {data.Date}");
                Console.ForegroundColor = ConsoleColor.Yellow;
                Console.WriteLine($"Лунный день: {data.LunarDay}");
                Console.ForegroundColor = ConsoleColor.Magenta;
                Console.WriteLine($"Фаза: {data.Phase}");
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Знак зодиака Луны: {data.ZodiacSign}");
                if (verbose && data.Recommendations != null)
                {
                    Console.ForegroundColor = ConsoleColor.White;
                    Console.WriteLine("Рекомендации:");
                    foreach (var kv in data.Recommendations)
                    {
                        if (kv.Value == "благоприятно") Console.ForegroundColor = ConsoleColor.Green;
                        else if (kv.Value == "неблагоприятно") Console.ForegroundColor = ConsoleColor.Red;
                        else Console.ForegroundColor = ConsoleColor.Yellow;
                        Console.WriteLine($"  - {kv.Key}: {kv.Value}");
                    }
                }
                Console.ResetColor();
            }
            else
            {
                Console.WriteLine($"🌙 Лунный календарь на {data.Date}");
                Console.WriteLine($"Лунный день: {data.LunarDay}");
                Console.WriteLine($"Фаза: {data.Phase}");
                Console.WriteLine($"Знак зодиака Луны: {data.ZodiacSign}");
                if (verbose && data.Recommendations != null)
                {
                    Console.WriteLine("Рекомендации:");
                    foreach (var kv in data.Recommendations)
                        Console.WriteLine($"  - {kv.Key}: {kv.Value}");
                }
            }
        }

        static async Task Main(string[] args)
        {
            string dateStr = null, jsonFile = null, csvFile = null;
            bool verbose = false, noColor = false;

            for (int i = 0; i < args.Length; i++)
            {
                switch (args[i])
                {
                    case "--date": dateStr = args[++i]; break;
                    case "--json": jsonFile = args[++i]; break;
                    case "--csv": csvFile = args[++i]; break;
                    case "--verbose": verbose = true; break;
                    case "--no-color": noColor = true; break;
                }
            }

            DateTime date = string.IsNullOrEmpty(dateStr) ? DateTime.UtcNow : DateTime.ParseExact(dateStr, "yyyy-MM-dd", null);
            var data = Calculate(date);
            bool color = !noColor && !Console.IsOutputRedirected;
            PrintData(data, verbose, color);

            if (jsonFile != null)
            {
                string json = JsonSerializer.Serialize(data, new JsonSerializerOptions { WriteIndented = true });
                await File.WriteAllTextAsync(jsonFile, json);
                Console.WriteLine($"Результат сохранён в {jsonFile}");
            }

            if (csvFile != null)
            {
                using var sw = new StreamWriter(csvFile);
                sw.WriteLine("date,lunar_day,phase,zodiac_sign,recommendations");
                string recStr = string.Join("; ", data.Recommendations.Select(kv => $"{kv.Key}:{kv.Value}"));
                sw.WriteLine($"{data.Date},{data.LunarDay},{data.Phase},{data.ZodiacSign},\"{recStr}\"");
                Console.WriteLine($"Результат сохранён в {csvFile}");
            }
        }
    }
}
