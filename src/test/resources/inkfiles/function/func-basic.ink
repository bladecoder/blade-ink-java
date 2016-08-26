VAR x = 0.0
~ x = lerp(2, 8, 0.3)
  The value of x is {x}.
  -> END
  
  === function lerp(a, b, k) ===
      ~ return ((b - a) * k) + a