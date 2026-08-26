// lunar_calendar.js
const { program } = require('commander');
const fs = require('fs');
const chalk = require('chalk');

const ZODIAC_SIGNS = ["Овен", "Телец", "Близнецы", "Рак", "Лев", "Дева",
                      "Весы", "Скорпион", "Стрелец", "Козерог", "Водолей", "Рыбы"];

const RECOMMENDATIONS = {
    "новолуние": { посадка: "неблагоприятно", полив: "допустимо", обрезка: "неблагоприятно", урожай: "неблагоприятно", вредители: "благоприятно" },
    "первая четверть": { посадка: "благоприятно", полив: "благоприятно", обрезка: "неблагоприятно", урожай: "неблагоприятно", вредители: "неблагоприятно" },
    "полнолуние": { посадка: "неблагоприятно", полив: "допустимо", обрезка: "благоприятно", урожай: "благоприятно", вредители: "благоприятно" },
    "последняя четверть": { посадка: "неблагоприятно", полив: "неблагоприятно", обрезка: "благоприятно", урожай: "благоприятно", вредители: "благоприятно" }
};

class LunarCalendar {
    constructor(dateStr) {
        this.date = dateStr ? new Date(dateStr + 'T00:00:00') : new Date();
        this.date.setHours(0, 0, 0, 0);
        this.calculate();
    }

    calculate() {
        // Базовое новолуние: 2000-01-06 18:14 UTC
        const base = new Date(Date.UTC(2000, 0, 6, 18, 14));
        const ms = this.date.getTime() - base.getTime();
        const days = ms / (1000 * 60 * 60 * 24);
        const lunarAge = days % 29.53058867;
        this.lunarDay = Math.floor(lunarAge) + 1;
        if (this.lunarDay > 30) this.lunarDay = 30;
        // Фаза
        if (this.lunarDay <= 1 || this.lunarDay > 29) this.phase = "новолуние";
        else if (this.lunarDay <= 7.4) this.phase = "первая четверть";
        else if (this.lunarDay <= 14.8) this.phase = "полнолуние";
        else if (this.lunarDay <= 22.2) this.phase = "последняя четверть";
        else this.phase = "новолуние";
        // Знак зодиака
        const start = new Date(Date.UTC(2000, 0, 1, 0, 0));
        const totalDays = (this.date.getTime() - start.getTime()) / (1000 * 60 * 60 * 24);
        let longitude = (180 + totalDays * 13.176) % 360;
        const signIndex = Math.floor(longitude / 30) % 12;
        this.zodiacSign = ZODIAC_SIGNS[signIndex];
    }

    getRecommendations() {
        return RECOMMENDATIONS[this.phase] || {};
    }

    toDict() {
        return {
            date: this.date.toISOString().split('T')[0],
            lunar_day: this.lunarDay,
            phase: this.phase,
            zodiac_sign: this.zodiacSign,
            recommendations: this.getRecommendations()
        };
    }

    print(verbose, color) {
        const c = color ? chalk : { cyan: s=>s, yellow: s=>s, magenta: s=>s, green: s=>s, white: s=>s, red: s=>s };
        console.log(c.cyan(`🌙 Лунный календарь на ${this.date.toISOString().split('T')[0]}`));
        console.log(c.yellow(`Лунный день: ${this.lunarDay}`));
        console.log(c.magenta(`Фаза: ${this.phase}`));
        console.log(c.green(`Знак зодиака Луны: ${this.zodiacSign}`));
        if (verbose) {
            console.log(c.white('Рекомендации:'));
            const rec = this.getRecommendations();
            for (const [key, val] of Object.entries(rec)) {
                let col = c.white;
                if (val === 'благоприятно') col = c.green;
                else if (val === 'неблагоприятно') col = c.red;
                else col = c.yellow;
                console.log(`  - ${key.charAt(0).toUpperCase()+key.slice(1)}: ${col(val)}`);
            }
        }
    }
}

program
    .option('-d, --date <date>', 'Дата (YYYY-MM-DD)')
    .option('--json <file>', 'Экспорт в JSON')
    .option('--csv <file>', 'Экспорт в CSV')
    .option('--verbose', 'Подробный вывод')
    .option('--no-color', 'Отключить цвет')
    .parse(process.argv);

const opts = program.opts();
const calendar = new LunarCalendar(opts.date);
const color = !opts.noColor && process.stdout.isTTY;
calendar.print(opts.verbose, color);

if (opts.json) {
    fs.writeFileSync(opts.json, JSON.stringify(calendar.toDict(), null, 2));
    console.log(`Результат сохранён в ${opts.json}`);
}

if (opts.csv) {
    const rec = calendar.getRecommendations();
    const recStr = Object.entries(rec).map(([k,v]) => `${k}:${v}`).join('; ');
    const line = `${calendar.date.toISOString().split('T')[0]},${calendar.lunarDay},${calendar.phase},${calendar.zodiacSign},"${recStr}"\n`;
    fs.writeFileSync(opts.csv, 'date,lunar_day,phase,zodiac_sign,recommendations\n' + line);
    console.log(`Результат сохранён в ${opts.csv}`);
}
