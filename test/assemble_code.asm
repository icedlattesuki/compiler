.text
.globl main
main:
li $t0, 1
la $t1, v1
li $t2, 0
add $t1, $t1, $t2
sw $t0, 0($t1)
li $t0, 2.0
la $t1, v1
li $t2, 4
add $t1, $t1, $t2
sw $t0, 0($t1)
la $t1, v1
li $t2, 0
add $t1, $t1, $t2
lw $t0, 0($t1)
move $v0, $t0
jr $ra
.data
v1: .space 8
