<#
    律法通 - 一键训练启动器 v1.0
    =============================
    功能：
    1. 自动关闭大内存无用进程（省出 ~1.2GB 空闲内存）
    2. 设置训练所需环境变量
    3. 启动 LLaMA-Factory 训练
    4. 训练完成后弹窗通知
    5. 可恢复被杀的进程
#>

$ScriptVersion = "1.0"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ConfigFile = Join-Path $ProjectRoot "train_config_batch_01.yaml"
$KilledLog = Join-Path $ProjectRoot ".killed_processes.txt"
$TrainLog = Join-Path $ProjectRoot "logs\training_session.log"

# 要杀掉的进程列表（进程名不含 .exe）
$KillTargets = @(
    "QQ",                    # 腾讯QQ
    "msedgewebview2",        # Edge 后台
    "MSPCManager",           # 电脑管家
    "MSPCManagerCore",
    "MSPCManagerService",
    "GCUService",            # 机械革命控制台
    "GCUBridge",
    "OfficeClickToRun",      # Office 自动更新
    "SearchIndexer",         # 搜索索引
    "PhoneExperienceHost",   # 手机连接
    "Widgets",               # 小部件
    "WidgetService",
    "ShiYeLine",             # 未知程序
    "Everything"             # 文件搜索
)

function Write-Banner {
    Clear-Host
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "   律法通 - 一键训练启动器 v$ScriptVersion" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host ""
}

function Get-FreeMemory {
    $os = Get-CimInstance Win32_OperatingSystem
    return @{
        FreeMB  = [math]::Round($os.FreePhysicalMemory / 1MB, 1)
        TotalMB = [math]::Round($os.TotalVisibleMemorySize / 1MB, 1)
        Pct     = [math]::Round($os.FreePhysicalMemory / $os.TotalVisibleMemorySize * 100, 0)
    }
}

function Show-MemoryStatus {
    $m = Get-FreeMemory
    Write-Host "  当前内存：$($m.FreeMB) GB 空闲 / $($m.TotalMB) GB 总计 ($($m.Pct)%)" -ForegroundColor $(
        if ($m.Pct -gt 50) { "Green" } elseif ($m.Pct -gt 30) { "Yellow" } else { "Red" }
    )
}

function Stop-UnusedProcesses {
    Write-Host "`n[Step 1/4] 关闭无用进程释放内存..." -ForegroundColor Yellow
    $killed = @()

    foreach ($name in $KillTargets) {
        $procs = Get-Process -Name $name -ErrorAction SilentlyContinue
        foreach ($p in $procs) {
            $mb = [math]::Round($p.WorkingSet64 / 1MB, 1)
            Write-Host "  [-] $($p.Name) (PID:$($p.Id)) - $mb MB" -ForegroundColor DarkYellow
            $killed += @{ Name = $p.Name; Id = $p.Id; Path = $p.Path }
            $p | Stop-Process -Force -ErrorAction SilentlyContinue
        }
    }

    if ($killed.Count -eq 0) {
        Write-Host "  没有找到可关闭的进程" -ForegroundColor Green
    } else {
        $totalMB = ($killed | ForEach-Object { (Get-Process -Id $_.Id -ErrorAction SilentlyContinue).WorkingSet64 } | Measure-Object -Sum).Sum
        $totalMB = [math]::Round($totalMB / 1MB, 1)
        Write-Host "  已关闭 $($killed.Count) 个进程" -ForegroundColor Green
    }

    # 保存被杀进程的备份（用于恢复）
    $killed | ConvertTo-Json | Set-Content -Path $KilledLog -Encoding UTF8

    Start-Sleep -Seconds 2
    Show-MemoryStatus
}

function Restore-KilledProcesses {
    if (-not (Test-Path $KilledLog)) {
        Write-Host "  没有找到进程备份文件，无法恢复" -ForegroundColor Yellow
        return
    }

    Write-Host "`n恢复被杀的进程..." -ForegroundColor Yellow
    $killed = Get-Content $KilledLog -Encoding UTF8 | ConvertFrom-Json

    foreach ($entry in $killed) {
        if ($entry.Path -and (Test-Path $entry.Path -ErrorAction SilentlyContinue)) {
            try {
                Start-Process -FilePath $entry.Path -WindowStyle Hidden
                Write-Host "  [+] $($entry.Name) - 已恢复" -ForegroundColor Green
            } catch {
                Write-Host "  [!] $($entry.Name) - 恢复失败: $_" -ForegroundColor DarkYellow
            }
        } else {
            Write-Host "  [?] $($entry.Name) - 路径未知，跳过恢复" -ForegroundColor DarkGray
        }
    }

    Remove-Item $KilledLog -Force -ErrorAction SilentlyContinue
}

function Set-TrainingEnv {
    Write-Host "`n[Step 2/4] 设置训练环境变量..." -ForegroundColor Yellow

    $env:HF_HUB_OFFLINE = "1"
    $env:HF_HOME = Join-Path $ProjectRoot "data\.hf_cache"

    # 创建缓存目录
    New-Item -ItemType Directory -Path $env:HF_HOME -Force | Out-Null

    Write-Host "  [+] HF_HUB_OFFLINE = 1" -ForegroundColor Green
    Write-Host "  [+] HF_HOME = $($env:HF_HOME)" -ForegroundColor Green
}

function Start-Training {
    Write-Host "`n[Step 3/4] 启动训练..." -ForegroundColor Yellow
    Write-Host "  配置文件：$ConfigFile" -ForegroundColor Gray
    Write-Host "  日志文件：$TrainLog" -ForegroundColor Gray
    Write-Host ""

    $CliExe = Join-Path $ProjectRoot "..\llmfactory_env\Scripts\llamafactory-cli.exe"
    $CliExe = Resolve-Path $CliExe

    if (-not (Test-Path $CliExe)) {
        Write-Host "  [ERR] 找不到 llamafactory-cli.exe" -ForegroundColor Red
        Write-Host "  预期路径：$CliExe" -ForegroundColor Red
        pause
        exit 1
    }

    if (-not (Test-Path $ConfigFile)) {
        Write-Host "  [ERR] 找不到训练配置文件" -ForegroundColor Red
        Write-Host "  预期路径：$ConfigFile" -ForegroundColor Red
        pause
        exit 1
    }

    $startTime = Get-Date
    Write-Host "  开始时间：$($startTime.ToString('yyyy-MM-dd HH:mm:ss'))" -ForegroundColor Gray
    Write-Host "  训练中，请耐心等待..." -ForegroundColor Green
    Write-Host "  (预计 20-30 分钟)" -ForegroundColor DarkYellow
    Write-Host ""

    # 启动训练（同步等待）
    $process = Start-Process -FilePath $CliExe `
        -ArgumentList "train", "`"$ConfigFile`"" `
        -NoNewWindow -Wait -PassThru

    $endTime = Get-Date
    $duration = ($endTime - $startTime).TotalMinutes

    Write-Host ""
    Write-Host "  结束时间：$($endTime.ToString('yyyy-MM-dd HH:mm:ss'))" -ForegroundColor Gray
    Write-Host "  耗时：$([math]::Round($duration, 1)) 分钟" -ForegroundColor Cyan

    if ($process.ExitCode -eq 0) {
        Write-Host "  训练状态：成功 ✅" -ForegroundColor Green
    } else {
        Write-Host "  训练状态：失败 (exit code: $($process.ExitCode)) ❌" -ForegroundColor Red
    }

    # Windows 弹窗通知
    try {
        $notification = New-Object -ComObject Wscript.Shell
        if ($process.ExitCode -eq 0) {
            $notification.Popup("Demo训练已完成！`n耗时 $([math]::Round($duration,1)) 分钟`n输出目录：outputs/batch_01", 0, "律法通 - 训练完成 ✅", 64)
        } else {
            $notification.Popup("训练失败 (exit code: $($process.ExitCode))`n请检查 logs/training_session.log", 0, "律法通 - 训练失败 ❌", 48)
        }
    } catch {
        Write-Host "  [!] 弹窗通知失败" -ForegroundColor DarkYellow
    }
}

function Show-Summary {
    Write-Host "`n[Step 4/4] 训练总结" -ForegroundColor Yellow
    Show-MemoryStatus

    Write-Host "`n============================================" -ForegroundColor Cyan
    Write-Host "  是否恢复被关闭的进程？" -ForegroundColor White
    Write-Host "============================================" -ForegroundColor Cyan
    $choice = Read-Host "  输入 Y 恢复 / N 不恢复 (默认: N)"

    if ($choice -eq "Y" -or $choice -eq "y") {
        Restore-KilledProcesses
        Show-MemoryStatus
    }

    Write-Host "`n按任意键退出..." -ForegroundColor Gray
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}

# ====== 主入口 ======
Write-Banner
Show-MemoryStatus
Stop-UnusedProcesses
Set-TrainingEnv
Start-Training
Show-Summary

# 自毁：删除本次计划任务（一次性任务，用完即毁）
schtasks /delete /tn "LvFaTong_Demo_Train" /f 2>$null