export function convertTimestamp(timestamp) {
    let date = new Date(timestamp)
    let Y = date.getFullYear() + '-'
    let M = (date.getMonth() + 1 < 10
        ? '0' + (date.getMonth() + 1)
        : date.getMonth() + 1) + '-'
    let D = date.getDate() + ' '
    let h = date.getHours() + ':'
    let mm = date.getMinutes() < 10
        ? '0' + date.getMinutes()
        : date.getMinutes()
    let m = mm + ':'
    let s = date.getSeconds() < 10
        ? '0' + date.getSeconds()
        : date.getSeconds()
    return Y + M + D + h + m + s
}