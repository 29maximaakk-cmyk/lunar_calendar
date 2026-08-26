// lunar_calendar.go
package main

import (
	"encoding/csv"
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

var zodiacSigns = []string{"Овен", "Телец", "Близнецы", "Рак", "Лев", "Дева",
	"Весы", "Скорпион", "Стрелец", "Козерог", "Водолей", "Рыбы"}

var recommendations = map[string]map[string]string{
	"новолуние":          {"посадка": "неблагоприятно", "полив": "допустимо", "обрезка": "неблагоприятно", "урожай": "неблагоприятно", "вредители": "благоприятно"},
	"первая четверть":    {"посадка": "благоприятно", "полив": "благоприятно", "обрезка": "неблагоприятно", "урожай": "неблагоприятно", "вредители": "неблагоприятно"},
	"полнолуние":         {"посадка": "неблагоприятно", "полив": "допустимо", "обрезка": "благоприятно", "урожай": "благоприятно", "вредители": "благоприятно"},
	"последняя четверть": {"посадка": "неблагоприятно", "полив": "неблагоприятно", "обрезка": "благоприятно", "урожай": "благоприятно", "вредители": "благоприятно"},
}

type LunarData struct {
	Date          string            `json:"date"`
	LunarDay      int               `json:"lunar_day"`
	Phase         string            `json:"phase"`
	ZodiacSign    string            `json:"zodiac_sign"`
	Recommendations map[string]string `json:"recommendations"`
}

func parseDate(dateStr string) time.Time {
	if dateStr == "" {
		return time.Now()
	}
	t, err := time.Parse("2006-01-02", dateStr)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Ошибка парсинга даты: %v\n", err)
		os.Exit(1)
	}
	return t
}

func calculate(date time.Time) LunarData {
	// Базовое новолуние: 2000-01-06 18:14 UTC
	base := time.Date(2000, 1, 6, 18, 14, 0, 0, time.UTC)
	diff := date.Sub(base).Hours() / 24
	lunarAge := diff - math.Floor(diff/29.53058867)*29.53058867
	lunarDay := int(lunarAge) + 1
	if lunarDay > 30 {
		lunarDay = 30
	}
	var phase string
	if lunarDay <= 1 || lunarDay > 29 {
		phase = "новолуние"
	} else if lunarDay <= 7 {
		phase = "первая четверть"
	} else if lunarDay <= 14 {
		phase = "полнолуние"
	} else if lunarDay <= 22 {
		phase = "последняя четверть"
	} else {
		phase = "новолуние"
	}
	// Знак зодиака
	start := time.Date(2000, 1, 1, 0, 0, 0, 0, time.UTC)
	totalDays := date.Sub(start).Hours() / 24
	longitude := 180 + totalDays*13.176
	longitude = math.Mod(longitude, 360)
	signIndex := int(math.Floor(longitude / 30)) % 12
	sign := zodiacSigns[signIndex]
	return LunarData{
		Date:          date.Format("2006-01-02"),
		LunarDay:      lunarDay,
		Phase:         phase,
		ZodiacSign:    sign,
		Recommendations: recommendations[phase],
	}
}

func printData(data LunarData, verbose bool, color bool) {
	if color {
		fmt.Printf("\033[36m🌙 Лунный календарь на %s\033[0m\n", data.Date)
		fmt.Printf("\033[33mЛунный день: %d\033[0m\n", data.LunarDay)
		fmt.Printf("\033[35mФаза: %s\033[0m\n", data.Phase)
		fmt.Printf("\033[32mЗнак зодиака Луны: %s\033[0m\n", data.ZodiacSign)
		if verbose {
			fmt.Println("\033[37mРекомендации:\033[0m")
			for k, v := range data.Recommendations {
				var col string
				if v == "благоприятно" {
					col = "\033[32m"
				} else if v == "неблагоприятно" {
					col = "\033[31m"
				} else {
					col = "\033[33m"
				}
				fmt.Printf("  - %s: %s%s\033[0m\n", k, col, v)
			}
		}
	} else {
		fmt.Printf("🌙 Лунный календарь на %s\n", data.Date)
		fmt.Printf("Лунный день: %d\n", data.LunarDay)
		fmt.Printf("Фаза: %s\n", data.Phase)
		fmt.Printf("Знак зодиака Луны: %s\n", data.ZodiacSign)
		if verbose {
			fmt.Println("Рекомендации:")
			for k, v := range data.Recommendations {
				fmt.Printf("  - %s: %s\n", k, v)
			}
		}
	}
}

func main() {
	var (
		dateStr string
		jsonOut string
		csvOut  string
		verbose bool
		noColor bool
	)
	flag.StringVar(&dateStr, "date", "", "Дата (YYYY-MM-DD)")
	flag.StringVar(&jsonOut, "json", "", "Экспорт в JSON")
	flag.StringVar(&csvOut, "csv", "", "Экспорт в CSV")
	flag.BoolVar(&verbose, "verbose", false, "Подробный вывод")
	flag.BoolVar(&noColor, "no-color", false, "Отключить цвет")
	flag.Parse()

	date := parseDate(dateStr)
	data := calculate(date)
	color := !noColor && isTerminal()
	printData(data, verbose, color)

	if jsonOut != "" {
		jsonData, err := json.MarshalIndent(data, "", "  ")
		if err != nil {
			fmt.Fprintf(os.Stderr, "Ошибка JSON: %v\n", err)
		} else {
			os.WriteFile(jsonOut, jsonData, 0644)
			fmt.Printf("Результат сохранён в %s\n", jsonOut)
		}
	}

	if csvOut != "" {
		f, err := os.Create(csvOut)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Ошибка CSV: %v\n", err)
		} else {
			defer f.Close()
			w := csv.NewWriter(f)
			defer w.Flush()
			w.Write([]string{"date", "lunar_day", "phase", "zodiac_sign", "recommendations"})
			recStr := ""
			for k, v := range data.Recommendations {
				if recStr != "" {
					recStr += "; "
				}
				recStr += k + ":" + v
			}
			w.Write([]string{data.Date, strconv.Itoa(data.LunarDay), data.Phase, data.ZodiacSign, recStr})
			fmt.Printf("Результат сохранён в %s\n", csvOut)
		}
	}
}

func isTerminal() bool {
	stat, _ := os.Stdout.Stat()
	return (stat.Mode() & os.ModeCharDevice) != 0
}
