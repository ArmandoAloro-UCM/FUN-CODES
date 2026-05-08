Details.cs
[fix] [refactor]

• Removed private object txt_email, was declared as wrong type with no matching TextBox on this form
• Added private string userEmail field to store email passed in from Form2
• Added string email = "" parameter to constructor so Form2 can pass the email through
• btn_select_Click now passes userEmail to Purchase instead of broken txt_email.Text


Form2.cs
[fix]

• Removed direct opening of Purchase with null for Details, would have crashed Confirm_Load
• Now opens Form3(txt_email.Text) after saving to DB, correctly starting the product selection flow


Form3.cs
[new]

• Added private string userEmail field
• Constructor now accepts string email = "" parameter
• All three product button handlers (btn_product1, btn_product2, btn_product3) now pass userEmail when opening product forms


product1.cs
[new]

• Added private string userEmail field
• Constructor now accepts string email = "" parameter
• btn_confirm_Click passes userEmail as last argument to Details() constructor


product2.cs
[new]

• Added private string userEmail field
• Constructor now accepts string email = "" parameter so Form3 can compile without error


product3.cs
[new]

• Added private string userEmail field
• Constructor now accepts string email = "" parameter so Form3 can compile without error
