// lunar_calendar.cpp
#include <iostream>
#include <string>
#include <vector>
#include <map>
#include <cmath>
#include <ctime>
#include <chrono>
#include <iomanip>
#include <fstream>
#include <sstream>
#include <json/json.h> // using jsoncpp

using namespace std;

const vector<string> ZODIAC_SIGNS = {"Овен","Телец","Близнецы","Рак","Лев","Дева",
                                     "Весы","Скорпион","Стрелец","Козерог","Водолей","Рыбы"};

const map<string, map<string, string>> RECOMMENDATIONS = {
    {"новолуние", {{"посадка","неблагоприятно"}, {"полив","допустимо"}, {"обрезка","неблагоприятно"}, {"урожай","неблагоприятно"}, {"вредители","благоприятно"}}},
    {"первая четверть", {{"посадка","благоприятно"}, {"полив","благоприятно"}, {"обрезка","неблагоприятно"}, {"урожай","неблагоприятно"}, {"вредители","неблагоприятно"}}},
    {"полнолуние", {{"посадка","неблагоприятно"}, {"полив","допустимо"}, {"обрезка","благоприятно"}, {"урожай","благоприятно"}, {"вредители","благоприятно"}}},
    {"последняя четверть", {{"посадка","неблагоприятно"}, {"полив","неблагоприятно"}, {"обрезка","благоприятно"}, {"урожай","благоприятно"}, {"вредители","благоприятно"}}}
};

struct LunarData {
    string date;
    int lunarDay;
    string phase;
    string zodiacSign;
    map<string, string> recommendations;
};

time_t parseDate(const string& dateStr) {
    tm tm = {};
    stringstream ss(dateStr);
    ss >> get_time(&tm, "%Y-%m-%d");
    if (ss.fail()) return time(nullptr);
    return mktime(&tm);
}

LunarData calculate(time_t timestamp) {
    // Базовое новолуние: 2000-01-06 18:14 UTC
    tm baseTm = {};
    baseTm.tm_year = 2000 - 1900;
    baseTm.tm_mon = 0;
    baseTm.tm_mday = 6;
    baseTm.tm_hour = 18;
    baseTm.tm_min = 14;
    time_t base = timegm(&baseTm);
    double diff = difftime(timestamp, base) / 86400.0;
    double lunarAge = fmod(diff, 29.53058867);
    int lunarDay = (int)floor(lunarAge) + 1;
    if (lunarDay > 30) lunarDay = 30;
    string phase;
    if (lunarDay <= 1 || lunarDay > 29) phase = "новолуние";
    else if (lunarDay <= 7) phase = "первая четверть";
    else if (lunarDay <= 14) phase = "полнолуние";
    else if (lunarDay <= 22) phase = "последняя четверть";
    else phase = "новолуние";
    // Знак зодиака
    tm startTm = {};
    startTm.tm_year = 2000 - 1900;
    startTm.tm_mon = 0;
    startTm.tm_mday = 1;
    time_t start = timegm(&startTm);
    double totalDays = difftime(timestamp, start) / 86400.0;
    double longitude = fmod(180 + totalDays * 13.176, 360);
    int signIndex = (int)floor(longitude / 30) % 12;
    string sign = ZODIAC_SIGNS[signIndex];

    LunarData data;
    // форматируем дату
    char buf[11];
    strftime(buf, sizeof(buf), "%Y-%m-%d", gmtime(&timestamp));
    data.date = buf;
    data.lunarDay = lunarDay;
    data.phase = phase;
    data.zodiacSign = sign;
    auto it = RECOMMENDATIONS.find(phase);
    if (it != RECOMMENDATIONS.end()) data.recommendations = it->second;
    return data;
}

void printData(const LunarData& data, bool verbose, bool color) {
    if (color) {
        cout << "\033[36m🌙 Лунный календарь на " << data.date << "\033[0m" << endl;
        cout << "\033[33mЛунный день: " << data.lunarDay << "\033[0m" << endl;
        cout << "\033[35mФаза: " << data.phase << "\033[0m" << endl;
        cout << "\033[32mЗнак зодиака Луны: " << data.zodiacSign << "\033[0m" << endl;
        if (verbose && !data.recommendations.empty()) {
            cout << "\033[37mРекомендации:\033[0m" << endl;
            for (auto& kv : data.recommendations) {
                string col;
                if (kv.second == "благоприятно") col = "\033[32m";
                else if (kv.second == "неблагоприятно") col = "\033[31m";
                else col = "\033[33m";
                cout << "  - " << kv.first << ": " << col << kv.second << "\033[0m" << endl;
            }
        }
    } else {
        cout << "🌙 Лунный календарь на " << data.date << endl;
        cout << "Лунный день: " << data.lunarDay << endl;
        cout << "Фаза: " << data.phase << endl;
        cout << "Знак зодиака Луны: " << data.zodiacSign << endl;
        if (verbose && !data.recommendations.empty()) {
            cout << "Рекомендации:" << endl;
            for (auto& kv : data.recommendations)
                cout << "  - " << kv.first << ": " << kv.second << endl;
        }
    }
}

void exportJSON(const LunarData& data, const string& filename) {
    Json::Value root;
    root["date"] = data.date;
    root["lunar_day"] = data.lunarDay;
    root["phase"] = data.phase;
    root["zodiac_sign"] = data.zodiacSign;
    for (auto& kv : data.recommendations) {
        root["recommendations"][kv.first] = kv.second;
    }
    ofstream ofs(filename);
    ofs << root.toStyledString();
    cout << "Результат сохранён в " << filename << endl;
}

void exportCSV(const LunarData& data, const string& filename) {
    ofstream ofs(filename);
    ofs << "date,lunar_day,phase,zodiac_sign,recommendations\n";
    string recStr;
    for (auto& kv : data.recommendations) {
        if (!recStr.empty()) recStr += "; ";
        recStr += kv.first + ":" + kv.second;
    }
    ofs << data.date << "," << data.lunarDay << "," << data.phase << "," << data.zodiacSign << ",\"" << recStr << "\"\n";
    cout << "Результат сохранён в " << filename << endl;
}

int main(int argc, char* argv[]) {
    string dateStr, jsonFile, csvFile;
    bool verbose = false, noColor = false;

    for (int i = 1; i < argc; ++i) {
        string arg = argv[i];
        if (arg == "--date" && i+1 < argc) dateStr = argv[++i];
        else if (arg == "--json" && i+1 < argc) jsonFile = argv[++i];
        else if (arg == "--csv" && i+1 < argc) csvFile = argv[++i];
        else if (arg == "--verbose") verbose = true;
        else if (arg == "--no-color") noColor = true;
    }

    time_t t = dateStr.empty() ? time(nullptr) : parseDate(dateStr);
    if (t == -1) {
        cerr << "Ошибка парсинга даты" << endl;
        return 1;
    }
    auto data = calculate(t);
    bool color = !noColor && isatty(fileno(stdout));
    printData(data, verbose, color);

    if (!jsonFile.empty()) exportJSON(data, jsonFile);
    if (!csvFile.empty()) exportCSV(data, csvFile);
    return 0;
}
