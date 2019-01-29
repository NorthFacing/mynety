#!/bin/bash
# 如果部署在服务器，sh脚本需要写绝对路径，才能在开机启动的时候启动成功
nohup sh ${app.name}.sh >/dev/null 2>&1 &
