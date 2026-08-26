// lunar_calendar.rs
use chrono::{DateTime, NaiveDate, Utc, TimeZone};
use clap::{App, Arg};
use serde::{Serialize, Deserialize};
use serde_json;
use std::fs;
use std::io::Write;
use std::collections::HashMap;
use colored::*;

const ZODIAC_SIGNS: [&str; 12] = ["Овен", "Телец", "Близнецы", "Рак", "Лев", "Дева",
                                  "Весы", "Скорпион", "Стрелец", "Козерог", "Водолей", "Рыбы"];

fn recommendations() -> HashMap<String, HashMap<String, String>> {
    let mut map = HashMap::new();
    map.insert("новолуние".to_string(), {
        let mut m = HashMap::new();
        m.insert("посадка".to_string(), "неблагоприятно".to_string());
        m.insert("полив".to_string(), "допустимо".to_string());
        m.insert("обрезка".to_string(), "неблагоприятно".to_string());
        m.insert("урожай".to_string(), "неблагоприятно".to_string());
        m.insert("вредители".to_string(), "благоприятно".to_string());
        m
    });
    map.insert("первая четверть".to_string(), {
        let mut m = HashMap::new();
        m.insert("посадка".to_string(), "благоприятно".to_string());
        m.insert("полив".to_string(), "благоприятно".to_string());
        m.insert("обрезка".to_string(), "неблагоприятно".to_string());
        m.insert("урожай".to_string(), "неблагоприятно".to_string());
        m.insert("вредители".to_string(), "неблагоприятно".to_string());
        m
    });
    map.insert("полнолуние".to_string(), {
        let mut m = HashMap::new();
        m.insert("посадка".to_string(), "неблагоприятно".to_string());
        m.insert("полив".to_string(), "допустимо".to_string());
        m.insert("обрезка".to_string(), "благоприятно".to_string());
        m.insert("урожай".to_string(), "благоприятно".to_string());
        m.insert("вредители".to_string(), "благоприятно".to_string());
        m
    });
    map.insert("последняя четверть".to_string(), {
        let mut m = HashMap::new();
        m.insert("посадка".to_string(), "неблагоприятно".to_string());
        m.insert("полив".to_string(), "неблагоприятно".to_string());
        m.insert("обрезка".to_string(), "благоприятно".to_string());
        m.insert("урожай".to_string(), "благоприятно".to_string());
        m.insert("вредители".to_string(), "благоприятно".to_string());
        m
    });
    map
}

#[derive(Serialize, Deserialize)]
struct LunarData {
    date: String,
    lunar_day: i32,
    phase: String,
    zodiac_sign: String,
    recommendations: HashMap<String, String>,
}

fn calculate(date: NaiveDate) -> LunarData {
    // Базовое новолуние: 2000-01-06 18:14 UTC
    let base = Utc.with_ymd_and_hms(2000, 1, 6, 18, 14, 0).unwrap();
    let dt = Utc.from_utc_datetime(&date.and_hms_opt(0,0,0).unwrap());
    let diff = dt - base;
    let days = diff.num_seconds() as f64 / 86400.0;
    let lunar_age = days % 29.53058867;
    let lunar_day = (lunar_age.floor() as i32) + 1;
    let lunar_day = if lunar_day > 30 { 30 } else { lunar_day };
    let phase = if lunar_day <= 1 || lunar_day > 29 {
        "новолуние".to_string()
    } else if lunar_day <= 7 {
        "первая четверть".to_string()
    } else if lunar_day <= 14 {
        "полнолуние".to_string()
    } else if lunar_day <= 22 {
        "последняя четверть".to_string()
    } else {
        "новолуние".to_string()
    };
    // Знак зодиака
    let start = Utc.with_ymd_and_hms(2000, 1, 1, 0, 0, 0).unwrap();
    let total_days = (dt - start).num_seconds() as f64 / 86400.0;
    let longitude = (180.0 + total_days * 13.176) % 360.0;
    let sign_index = ((longitude / 30.0).floor() as usize) % 12;
    let zodiac_sign = ZODIAC_SIGNS[sign_index].to_string();

    let rec_map = recommendations();
    let recs = rec_map.get(&phase).cloned().unwrap_or(HashMap::new());

    LunarData {
        date: date.format("%Y-%m-%d").to_string(),
        lunar_day,
        phase,
        zodiac_sign,
        recommendations: recs,
    }
}

fn print_data(data: &LunarData, verbose: bool, color: bool) {
    if color {
        println!("{}", format!("🌙 Лунный календарь на {}", data.date).cyan());
        println!("{}", format!("Лунный день: {}", data.lunar_day).yellow());
        println!("{}", format!("Фаза: {}", data.phase).magenta());
        println!("{}", format!("Знак зодиака Луны: {}", data.zodiac_sign).green());
        if verbose {
            println!("{}", "Рекомендации:".white());
            for (k, v) in &data.recommendations {
                let col = if v == "благоприятно" { "green" } else if v == "неблагоприятно" { "red" } else { "yellow" };
                let colored = match col {
                    "green" => v.green(),
                    "red" => v.red(),
                    _ => v.yellow(),
                };
                println!("  - {}: {}", k, colored);
            }
        }
    } else {
        println!("🌙 Лунный календарь на {}", data.date);
        println!("Лунный день: {}", data.lunar_day);
        println!("Фаза: {}", data.phase);
        println!("Знак зодиака Луны: {}", data.zodiac_sign);
        if verbose {
            println!("Рекомендации:");
            for (k, v) in &data.recommendations {
                println!("  - {}: {}", k, v);
            }
        }
    }
}

fn main() {
    let matches = App::new("Лунный календарь")
        .arg(Arg::with_name("date").long("date").takes_value(true).help("Дата YYYY-MM-DD"))
        .arg(Arg::with_name("json").long("json").takes_value(true).help("Экспорт в JSON"))
        .arg(Arg::with_name("csv").long("csv").takes_value(true).help("Экспорт в CSV"))
        .arg(Arg::with_name("verbose").long("verbose").help("Подробный вывод"))
        .arg(Arg::with_name("no-color").long("no-color").help("Отключить цвет"))
        .get_matches();

    let date_str = matches.value_of("date").unwrap_or("");
    let date = if date_str.is_empty() {
        chrono::Local::now().date_naive()
    } else {
        NaiveDate::parse_from_str(date_str, "%Y-%m-%d").expect("Ошибка формата даты")
    };
    let data = calculate(date);
    let color = !matches.is_present("no-color") && atty::is(atty::Stream::Stdout);
    print_data(&data, matches.is_present("verbose"), color);

    if let Some(json_file) = matches.value_of("json") {
        let json = serde_json::to_string_pretty(&data).unwrap();
        fs::write(json_file, json).unwrap();
        println!("Результат сохранён в {}", json_file);
    }

    if let Some(csv_file) = matches.value_of("csv") {
        let mut wtr = csv::Writer::from_path(csv_file).unwrap();
        wtr.write_record(&["date", "lunar_day", "phase", "zodiac_sign", "recommendations"]).unwrap();
        let rec_str = data.recommendations.iter()
            .map(|(k,v)| format!("{}:{}", k, v))
            .collect::<Vec<_>>().join("; ");
        wtr.write_record(&[data.date, data.lunar_day.to_string(), data.phase, data.zodiac_sign, rec_str]).unwrap();
        wtr.flush().unwrap();
        println!("Результат сохранён в {}", csv_file);
    }
}
