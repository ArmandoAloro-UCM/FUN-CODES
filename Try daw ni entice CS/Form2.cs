using System;
using System.Data.SqlClient;
using System.Windows.Forms;
using static System.Windows.Forms.VisualStyles.VisualStyleElement.ListView;

namespace APPSDEV_PROJECT
{
    public partial class Form2 : Form
    {
        SqlConnection sqlConn = new SqlConnection(
            @"Data Source=(localdb)\MSSQLLocalDB;Initial Catalog=UserDB;Integrated Security=True;"
        );


        public Form2()
        {
            InitializeComponent();
        }

        private void btn_confirm_Click(object sender, EventArgs e)
        {

            try
            {
                using (SqlConnection sqlConn = new SqlConnection(@"Data Source=(localdb)\MSSQLLocalDB;Initial Catalog=UserDB;Integrated Security=True;"))
                {
                    sqlConn.Open();

                    // First try UPDATE
                    string updateQuery = @"UPDATE [Users] 
                                           SET Name=@Name, Age=@Age, Gender=@Gender, 
                                           Mobile_Number=@Mobile, CheckInDate=@In, 
                                           CheckOutDate=@Out, NumberOfGuest=@Guest
                                           WHERE Email=@Email";

                    using (SqlCommand updateCmd = new SqlCommand(updateQuery, sqlConn))
                    {
                        updateCmd.Parameters.AddWithValue("@Name", txt_name.Text);
                        updateCmd.Parameters.AddWithValue("@Age", txt_age.Text);
                        updateCmd.Parameters.AddWithValue("@Gender", txt_gender.Text);
                        updateCmd.Parameters.AddWithValue("@Mobile", txt_number.Text);
                        updateCmd.Parameters.AddWithValue("@In", txt_chkIn.Text);
                        updateCmd.Parameters.AddWithValue("@Out", txt_chkout.Text);
                        updateCmd.Parameters.AddWithValue("@Guest", txt_guest.Text);
                        updateCmd.Parameters.AddWithValue("@Email", txt_email.Text);

                        int rowsAffected = updateCmd.ExecuteNonQuery();

                        if (rowsAffected == 0)
                        {
                            // If no row was updated, INSERT new record
                            string insertQuery = @"INSERT INTO [Users] 
                                                 (Name, Age, Gender, Mobile_Number, Email, CheckInDate, CheckOutDate, NumberOfGuest) 
                                                 VALUES (@Name, @Age, @Gender, @Mobile, @Email, @In, @Out, @Guest)";

                            using (SqlCommand insertCmd = new SqlCommand(insertQuery, sqlConn))
                            {
                                insertCmd.Parameters.AddWithValue("@Name", txt_name.Text);
                                insertCmd.Parameters.AddWithValue("@Age", txt_age.Text);
                                insertCmd.Parameters.AddWithValue("@Gender", txt_gender.Text);
                                insertCmd.Parameters.AddWithValue("@Mobile", txt_number.Text);
                                insertCmd.Parameters.AddWithValue("@Email", txt_email.Text);
                                insertCmd.Parameters.AddWithValue("@In", txt_chkIn.Text);
                                insertCmd.Parameters.AddWithValue("@Out", txt_chkout.Text);
                                insertCmd.Parameters.AddWithValue("@Guest", txt_guest.Text);

                                insertCmd.ExecuteNonQuery();
                                MessageBox.Show("User added successfully!");
                            }
                        }
                        else
                        {
                            MessageBox.Show("User updated successfully!");
                        }
                    }
                }
            }
            catch (SqlException ex)
            {
                MessageBox.Show("Database error: " + ex.Message);
            }



            this.Hide();
            Form3 f3 = new Form3(txt_email.Text); // pass email to Form3
            f3.ShowDialog();
            this.Show();


        }


        /// AYAW NI HILABTI KAI MA GUBA TANAN
        private void Form2_Load(object sender, EventArgs e)
        {
        }
    }
}
