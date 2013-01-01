(begin
  (define list (lambda args args))
  (define test1 (lambda (x)
     (display (list "hello there" x 57))
     (newline)))
 (test1 23))



