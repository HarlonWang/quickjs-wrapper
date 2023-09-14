import * as fs from 'node:fs';
import * as path from 'node:path';
import { exec as _exec } from 'node:child_process';
import * as util from "node:util"
import * as process from "node:process"

export function readdir(path) {
    return fs.readdirSync(path)
}

export function loadFile(path) {
    return fs.readFileSync(path, "utf-8")
}

export function mkdir(path) {
    fs.mkdirSync(path);
}

export function writeToFile(path, source) {
    fs.writeFileSync(path, source);
}

export function removeFile(path) {
    fs.unlinkSync(path);
}

export function removeDir(dir) {
    let files = fs.readdirSync(dir)
    for (let i = 0; i < files.length; i++) {
        let newPath = path.join(dir, files[i]);
        let stat = fs.statSync(newPath)
        if (stat.isDirectory()) {
            //如果是文件夹就递归下去
            removeDir(newPath);
        } else {
            //删除文件
            fs.unlinkSync(newPath);
        }
    }
    fs.rmdirSync(dir)//如果文件夹是空的，就将自己删除掉
}

export function existDir(dir) {
    return fs.existsSync(dir)
}

export function exec(command, callback) {
    _exec(command, callback)
}

export function utilFormat(...args) {
    return util.format(...args)
}

export function printWithUpdate(text) {
    process.stdout.clearLine()
    process.stdout.cursorTo(0)
    process.stdout.write(text);
}

export function cwd() {
    return process.cwd()
}
